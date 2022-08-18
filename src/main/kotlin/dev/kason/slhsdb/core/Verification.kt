package dev.kason.slhsdb.core

import arrow.core.Either
import dev.kason.slhsdb.database
import dev.kason.slhsdb.encodeBase64Url
import dev.kason.slhsdb.random
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import net.axay.simplekotlinmail.delivery.MailerManager
import net.axay.simplekotlinmail.delivery.mailerBuilder
import net.axay.simplekotlinmail.delivery.send
import net.axay.simplekotlinmail.email.emailBuilder
import org.litote.kmongo.lt
import org.simplejavamail.api.email.EmailPopulatingBuilder
import kotlin.time.Duration.Companion.hours

private var username: String? = null

fun registerMailer() {
    username = System.getProperty("email.username")
    MailerManager.defaultMailer = mailerBuilder(
        host = "smtp.gmail.com",
        port = 587,
        username = username,
        password = System.getProperty("email.password")
    ) {
        properties["mail.smtp.host"] = "smtp.gmail.com"
        properties["mail.smtp.port"] = "587"
        properties["mail.transport.protocol"] = "smtp"
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.starttls.required"] = "true"
    }
}

suspend fun EmailPopulatingBuilder.useUsername(): EmailPopulatingBuilder {
    if (username == null) registerMailer()
    return from(username!!)
}

@kotlinx.serialization.Serializable
data class Verification(
    @SerialName("_id")
    val token: String,
    val discordId: Snowflake,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val studentCode: String,
    val preferredName: String?,
    val expirationDate: Instant
) {
}

private val verificationCollection = database.getCollection<Verification>()

suspend fun verification(
    discordId: Snowflake,
    firstName: String,
    middleName: String? = null,
    lastName: String,
    studentCode: String,
    preferredName: String?
): Verification {
    val verification = Verification(
        token = random.nextBytes(15).encodeBase64Url(),
        discordId = discordId,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        studentCode = studentCode,
        preferredName = preferredName,
        expirationDate = Clock.System.now() + 1.hours
    )
    emailBuilder {
        useUsername()
        to(studentCode.lowercase() + "@students.katyisd.org")
        withSubject("SLHS Discord Bot verification")
        withPlainText("Your verification code is ${verification.token}. This token expires in 1 hour.")
    }.send().join()
    verificationCollection.insertOne(verification)
    return verification
}

suspend fun finish(token: String, discordMessenger: Snowflake): Either<String, Student> {
    val verification = verificationCollection.findOneById(token)!!
    verificationCollection.deleteMany(Verification::expirationDate lt Clock.System.now())
    if (discordMessenger != verification.discordId) {
        return Either.Left("Please use the same discord account you used to register.")
    }
    if (verification.expirationDate < Clock.System.now()) {
        return Either.Left("Verification has expired already.")
    }
    val student = Student(
        discordId = verification.discordId,
        firstName = verification.firstName,
        middleName = verification.middleName,
        lastName = verification.lastName,
        studentCode = verification.studentCode,
        preferredName = verification.preferredName
    )
    studentDatabase.insertOne(student)
    return Either.Right(student)
}