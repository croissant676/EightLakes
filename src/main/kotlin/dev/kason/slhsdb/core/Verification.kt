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

fun EmailPopulatingBuilder.useUsername(): EmailPopulatingBuilder {
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
        token = random.nextBytes(24).encodeBase64Url(),
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
        withHTMLText(
            """<html lang="en"><head> <meta charset="UTF-8"> <style> body { background: white; font-family: 'Dubai-Light',
                 sans-serif; text-align: center; padding: 10%; margin: 0 auto; } .main { background: aliceblue; padding: 10%; }
                  strong { color: orange; } h6 { font-size: 20px; } .bottom { padding: 1%; display: flex; width: 100%; height: 100%;
                   justify-content: center; align-items: center; } p { padding: 5px; width: auto; font-size: 20px; background: white; }
                    p:hover { background: #c3e4ff; } button { padding: 5px; font-family: 'Dubai-Light', sans-serif; text-align: center;
                     font-size: 20px; background: white; border: white solid; width: auto; } button:hover { background: #c3e4ff;
                      color: white; } </style></head><body><div class="main"> <h6> Your auth token is: </h6> <h1> <strong> ${verification.token}
                       </strong> </h1> <h6> This token expires in <strong>1</strong> hour. </h6></div><div class="bottom"> <p>
                        Created for the SLHS Discord bot. </p> <button onclick="github()"> Click here to view source on Github </button>
                        </div><script> function github() { open("https://github.com/croissant676/BreakTracker") }</script></body></html>""".trimIndent()
        )
    }.send().join()
    verificationCollection.insertOne(verification)
    return verification
}

suspend fun finish(token: String, discordMessenger: Snowflake): Either<String, Student> {
    val verification = verificationCollection.findOneById(token)!!
    if (discordMessenger != verification.discordId) {
        return Either.Left("Please use the same discord account you used to register.")
    }
    if (verification.expirationDate < Clock.System.now()) {
        return Either.Left("Your verification has expired already.")
    }
    val student = Student(
        discordId = verification.discordId,
        firstName = verification.firstName,
        middleName = verification.middleName,
        lastName = verification.lastName,
        studentCode = verification.studentCode,
        preferredName = verification.preferredName,
        memberNumber = 0
    )
    updateStudentJoinValues()
    studentDatabase.insertOne(student)
    return Either.Right(student)
}