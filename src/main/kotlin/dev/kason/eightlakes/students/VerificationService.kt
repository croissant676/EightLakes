package dev.kason.eightlakes.students

import dev.kason.eightlakes.utils.ConfigAware
import freemarker.template.Configuration
import io.ktor.util.*
import kotlinx.datetime.Clock
import mu.KLogging
import net.axay.simplekotlinmail.delivery.*
import net.axay.simplekotlinmail.email.emailBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*
import uy.klutter.config.typesafe.value
import java.io.StringWriter
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

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
        sendEmail(verification, student)
        return verification
    }

    private val template by lazy(LazyThreadSafetyMode.NONE) {
        freemarkerConfig.getTemplate("verification.ftl")
    }

    private suspend fun sendEmail(verification: StudentVerification, student: Student) {
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
}