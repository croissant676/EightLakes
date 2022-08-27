package dev.kason.eightlakes.core

import arrow.core.left
import arrow.core.right
import dev.kason.eightlakes.applicationClass
import dev.kason.eightlakes.core.models.Student
import dev.kason.eightlakes.core.models.StudentVerification
import dev.kason.eightlakes.core.models.Students
import dev.kason.eightlakes.core.utils.Possible
import dev.kason.eightlakes.core.utils.createSqlOp
import dev.kason.eightlakes.random
import dev.kord.common.entity.Snowflake
import io.ktor.util.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.and
import java.util.*
import kotlin.text.isLowerCase
import kotlin.time.Duration.Companion.hours

// Returns either a message about why it failed, or a student
suspend fun signupStudent(
    _studentId: String,
    _firstName: String,
    _middleName: String?,
    _lastName: String,
    _preferredName: String?,
    discordSnowflake: Snowflake
): Possible<Student> {
    val startingChar = _studentId.first()
    if (!startingChar.isLetter())
        return "Your student ID should start with a letter.".left()
    val idNumbers = _studentId.drop(1)
    if (idNumbers.any { !it.isDigit() })
        return "Your student ID should contain only numbers aside from the first letter.".left()
    val studentId = startingChar + idNumbers
    if (Student.count(createSqlOp { Students.studentId eq studentId }) > 0)
        return "A student with the same ID has already been signed up.".left()
    val firstName =
        _firstName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val middleName =
        _middleName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val lastName =
        _lastName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val preferredName =
        _preferredName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    if (Student.count(createSqlOp {
            (Students.firstName eq firstName) and (Students.middleName eq middleName) and (Students.lastName eq lastName)
        }) > 0) return "A student with the same first name, middle name, and last name has already signed up".left()
    val newStudent = Student.new {
        this.firstName = firstName
        this.middleName = middleName
        this.lastName = lastName
        this.preferredName = preferredName
        this.discordId = discordSnowflake
    }
    val verificationCode = StudentVerification.new {
        this.student = student
        this.token = random.nextBytes(24).encodeBase64()
        this.expirationDate = Clock.System.now() + 1.hours
    }
    sendEmailVerification(newStudent, verificationCode)
    return newStudent.right()
}

// The HTML for the email
// If result == null, completed successfully, otherwise, returns an error message.
suspend fun sendEmailVerification(student: Student, verification: StudentVerification): String? = runCatching {
    sendEmailTo(student.email) {
        val url = applicationClass.getResource("/email.html")?.file

    }
    return@runCatching null
}.getOrElse { "The email could not be sent." }
