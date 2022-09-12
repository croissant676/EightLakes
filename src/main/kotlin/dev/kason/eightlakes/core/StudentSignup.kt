package dev.kason.eightlakes.core

import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.discord.*
import dev.kason.eightlakes.random
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Role
import io.ktor.util.*
import kotlinx.datetime.*
import net.axay.simplekotlinmail.delivery.send
import net.axay.simplekotlinmail.email.emailBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import kotlin.time.Duration.Companion.hours

private val studentIdRegex = Regex("[A-Za-z]\\d{7}")

suspend fun registerStudent(
    _firstName: String,
    _middleName: String?,
    _lastName: String,
    _preferredName: String?,
    _studentId: String,
    birthday: LocalDate,
    discordId: Snowflake
): Student {
    if (!(studentIdRegex matches _studentId)) illegalArg("The student id `$_studentId` is not a valid student id.")
    val studentId = _studentId.lowercase().replaceFirstChar(Char::uppercase)
    val firstName = _firstName.lowercase().replaceFirstChar(Char::uppercase)
    val middleName = _middleName?.lowercase()?.replaceFirstChar(Char::uppercase)
    val lastName = _lastName.lowercase().replaceFirstChar(Char::uppercase)
    val preferredName = _preferredName?.lowercase()?.replaceFirstChar(Char::uppercase)
    val failsChecks = suspendTransaction {
        val nameCount = Student.count(
            (Students.firstName eq firstName) and
                    (Students.middleName eq middleName) and
                    (Students.lastName eq lastName)
        )
        val discordCount = Student.count(Students.discordId eq discordId)
        val idCount = Student.count(Students.studentId eq studentId)
        listOf(nameCount, discordCount, idCount)
    }.any { it > 0 }
    if (failsChecks) illegalArg("A student with the same name, discord, or student id has already signed up.")
    val newStudent = suspendTransaction {
        Student.new {
            this.firstName = firstName
            this.middleName = middleName
            this.lastName = lastName
            this.preferredName = preferredName
            this.studentId = studentId
            this.birthday = birthday
            this.discordId = discordId
            this.isVerified = false
        }
    }
    createVerificationFor(newStudent)
    return newStudent
}

private suspend fun createVerificationFor(student: Student) {
    val email = student.email
    val newVerification = suspendTransaction {
        StudentVerification.new {
            this.student = student
            this.email = email
            this.token = random.nextBytes(24).encodeBase64()
            this.expirationDate = Clock.System.now() + 1.hours
        }
    }
    emailBuilder {
        to(email)
        from(emailUsername)
        withSubject("EightLakes verification")
        withPlainText("Placeholder plain text.... token = ${newVerification.token}")
    }.send()
}

private var _verifiedRole: Role? = null
suspend fun verifiedRole(): Role = _verifiedRole ?: role {
    this.name = "Los Pobrecitos"
    this.color = Color(18, 207, 132)
}.also { _verifiedRole = it }

suspend fun finishVerification(
    token: String,
    discordId: Snowflake
): Student = suspendTransaction {
    val verification = StudentVerification.find(StudentVerifications.token eq token).toList().firstOrNull()
        ?: illegalArg("Could not find a verification with the token $token.")
    if (verification.isExpired) illegalArg("The verification has already expired.")
    val student = verification.student
    if (student.discordId != discordId) {
        illegalArg("Please use the same account as the one you used to signup.")
    }
    val member = student.member()
    member.addRole(verifiedRole().id, "Student has been verified")
    return@suspendTransaction student
}
