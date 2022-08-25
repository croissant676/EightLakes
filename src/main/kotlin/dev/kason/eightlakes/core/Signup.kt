package dev.kason.eightlakes.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.kason.eightlakes.core.models.Student
import dev.kason.eightlakes.core.models.Students
import dev.kord.common.entity.Snowflake
import kotlinx.html.*
import net.axay.simplekotlinmail.html.withHTML
import org.jetbrains.exposed.sql.and
import java.util.*

// Returns either a message about why it failed, or a student
suspend fun signupStudent(
    _studentId: String,
    _firstName: String,
    _middleName: String?,
    _lastName: String,
    _preferredName: String?,
    discordSnowflake: Snowflake
): Either<String, Student> {
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
    sendEmailHTML(newStudent)
    return newStudent.right()
}

// The HTML for the email
suspend fun sendEmailHTML(student: Student) {
    sendEmailTo(student.email) {
        withHTML {
            head {
                link("") {

                }
            }
            body {
                div {
                }
                div {
                    p("") {
                        +"Created for the Eight Lakes Discord Bot project."
                    }
                    a("https://github.com/croissant676/EightLakes") {

                    }
                }
            }
        }
    }
}