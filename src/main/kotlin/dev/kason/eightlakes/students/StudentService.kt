package dev.kason.eightlakes.students

import dev.kason.eightlakes.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class StudentService(override val di: DI) : DIAware {
    companion object : KLogging()

    private val studentIdRegex = Regex("[A-Za-z]\\d{7}")

    suspend fun signup(
        firstName: String,
        middleName: String?,
        lastName: String,
        preferredName: String?,
        studentId: String,
        birthdayString: String,
        discordUser: User
    ): Student {
        require(studentId matches studentIdRegex) { "$studentId is not a valid id." }
        val capitalizedId = studentId.capitalize()
        val capitalizedFirst = firstName.capitalize()
        val capitalizedMiddle = middleName?.capitalize()
        val capitalizedLast = lastName.capitalize()
        newSuspendedTransaction {
            require(uniqueName(capitalizedFirst, capitalizedMiddle, capitalizedLast)) {
                "There is a student with the same name (first, middle, last) signed up already."
            }
            require(uniqueStudentId(capitalizedId)) {
                "There is a student with the same student id signed up already."
            }
            require(uniqueDiscordId(discordUser.id)) {
                "There is a student with the same discord account signed up already."
            }
        }
        val birthday = birthdayString.localDate()
        val student = Student.new {
            this.firstName = capitalizedFirst
            this.middleName = capitalizedMiddle
            this.lastName = capitalizedLast
            this.preferredName = preferredName?.capitalize()
            this.studentId = capitalizedId
            this.discordId = discordUser.id
            this.birthday = birthday
        }
        return student
    }

    private fun none(condition: Op<Boolean>): Boolean =
        Student.count(condition) == 0L

    private fun uniqueName(
        firstName: String,
        middleName: String?,
        lastName: String
    ): Boolean = none(
        (Students.firstName eq firstName) and
                (Students.middleName eq middleName) and
                (Students.lastName eq lastName)
    )

    private fun uniqueStudentId(
        studentId: String
    ): Boolean = none(
        Students.studentId eq studentId
    )

    private fun uniqueDiscordId(
        discordId: Snowflake
    ): Boolean = none(
        Students.discordId eq discordId
    )

}