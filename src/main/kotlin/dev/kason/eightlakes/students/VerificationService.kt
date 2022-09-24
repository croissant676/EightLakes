package dev.kason.eightlakes.students

import dev.kason.eightlakes.ConfigAware
import dev.kord.core.entity.Member
import freemarker.template.*
import io.ktor.util.*
import kotlinx.datetime.Clock
import mu.KLogging
import net.axay.simplekotlinmail.delivery.*
import net.axay.simplekotlinmail.email.emailBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*
import uy.klutter.config.typesafe.value
import java.io.StringWriter
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

@Suppress("DuplicatedCode")
class VerificationService(override val di: DI) : ConfigAware(di) {
    companion object : KLogging() {
        @Suppress("LocalVariableName")
        val ExpirationDuration = 1.hours
    }

    private val freemarkerConfig: Configuration by di.instance()
    private var emailUsername: String? = null // null = uninitialized

    private fun createMailerIfNotCreated() {
        if (emailUsername != null) return
        val host = config.value("email.host").asString("localhost")
        val port = config.value("email.port").asInt(25)
        val password = config.value("email.password").asStringOrNull()
        emailUsername = config.getString("email.username")
        MailerManager.defaultMailer = mailerBuilder(host, port, emailUsername, password) {
            val propsConfig = config.getConfig("email.props")
                ?: return@mailerBuilder
            val propsAsMap = propsConfig.entrySet().associate { it.key to propsConfig.getString(it.key) }
            for ((string, configuredValue) in propsAsMap) {
                properties["mail.$string"] = configuredValue
            }
            logger.debug { "Inserted properties $propsAsMap into mailer.properties" }
        }
    }

    suspend fun openVerification(
        student: Student
    ): StudentVerification {
        deleteAllExpiredVerifications()
        createMailerIfNotCreated()
        val email = student.email
        val verification = newSuspendedTransaction {
            StudentVerification.new {
                this.student = student
                this.token = Random.nextBytes(24).encodeBase64()
                this.email = email
                this.expirationDate = Clock.System.now() + ExpirationDuration
            }
        }
        sendEmail(verification, student, firstTimeTemplate)
        return verification
    }

    private suspend fun hasPreviousVerification(student: Student): Boolean = newSuspendedTransaction {
        StudentVerification.find {
            (StudentVerifications.student eq student.id) and (StudentVerifications.expirationDate greater CurrentDateTime)
        }.count() > 0
    }

    suspend fun openAdditionalVerification(
        student: Student
    ): StudentVerification {
        deleteAllExpiredVerifications()
        createMailerIfNotCreated()
        require(!hasPreviousVerification(student)) { "You already have a verification that hasn't expired. Use that one instead." }
        val email = student.email
        val verification = newSuspendedTransaction {
            StudentVerification.new {
                this.student = student
                this.token = Random.nextBytes(24).encodeBase64()
                this.email = email
                this.expirationDate = Clock.System.now() + ExpirationDuration
            }
        }
        sendEmail(verification, student, additionalTemplate)
        return verification
    }

    private suspend fun deleteAllExpiredVerifications() {
        newSuspendedTransaction {
            StudentVerification.find {
                StudentVerifications.expirationDate less CurrentDateTime
            }.forEach { it.delete() }
        }
    }

    private val firstTimeTemplate by lazy(LazyThreadSafetyMode.NONE) {
        freemarkerConfig.getTemplate("verification.ftl")
    }
    private val additionalTemplate by lazy(LazyThreadSafetyMode.NONE) {
        freemarkerConfig.getTemplate("verification_additional.ftl")
    }

    private suspend fun sendEmail(
        verification: StudentVerification,
        student: Student,
        template: Template
    ) {
        emailBuilder {
            from(emailUsername!!)
            to(verification.email)
            withSubject("Eight Lakes verification")
            val stringWriter = StringWriter()
            template.process(
                mapOf(
                    "name" to student.preferredOrFirst,
                    "token" to verification.token,
                    "email" to student.email
                ), stringWriter
            )
            withHTMLText(stringWriter.toString())
        }.send()
    }

    suspend fun close(token: String, user: Member) = newSuspendedTransaction {
        val verification =
            StudentVerification.find(StudentVerifications.token eq token).first()
        require(Clock.System.now() < verification.expirationDate) { "Token has expired already. " }
        val student = verification.student
        require(user.id == student.discordId) { "Please use the same discord account that used to sign up." }
        verification.delete()
        student.isVerified = true
    }
}