package dev.kason.eightlakes.students

import dev.kason.eightlakes.*
import dev.kason.eightlakes.courses.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.*
import dev.kord.rest.builder.message.EmbedBuilder
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

@Suppress("MemberVisibilityCanBePrivate")
class StudentService(override val di: DI) : DIAware, DiscordEntityService<Student> {
    companion object : KLogging()

    private val studentIdRegex = Regex("[A-Za-z]\\d{7}")
    private val verificationService: VerificationService by instance()
    private val guild: Guild by instance()

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
        val capitalizedId = studentId.capitalized()
        val capitalizedFirst = firstName.capitalized()
        val capitalizedMiddle = middleName?.capitalized()
        val capitalizedLast = lastName.capitalized()
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
        val student = newSuspendedTransaction {
            Student.new {
                this.firstName = capitalizedFirst
                this.middleName = capitalizedMiddle
                this.lastName = capitalizedLast
                this.preferredName = preferredName?.capitalized()
                this.studentId = capitalizedId
                this.discordId = discordUser.id
                this.birthday = birthday
            }
        }
        verificationService.openVerification(student)
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

    override suspend fun get(discordId: Snowflake): Student = requireNotNull(getOrNull(discordId)) {
        "Student with discord id $discordId does not exist."
    }

    override suspend fun getOrNull(discordId: Snowflake): Student? =
        newSuspendedTransaction {
            Student.find { Students.discordId eq discordId }.firstOrNull()
        }

    suspend fun getSchedule(student: Student): Map<Period, CourseClass> {
        val map = mutableMapOf<Period, CourseClass>()
        newSuspendedTransaction {
            val studentCourseClasses = StudentClass.find { StudentClasses.student eq student.id }
            studentCourseClasses.forEach {
                val courseclass = it.courseClass
                val period = courseclass.period
                map[period] = courseclass
            }
        }
        return map
    }

    @Deprecated("use createScheduleEmbed instead; also doesn't work", ReplaceWith("createScheduleEmbed(student)"))
    suspend fun createScheduleTextBox(student: Student) = buildString {
        val schedule = getSchedule(student)
        appendLine("```")
        val max = schedule.values.maxOfOrNull { it.scheduleDescription.length } ?: 0
        val lengthOfTable = max + 13
        appendLine("+" + "-".repeat(lengthOfTable - 2) + "+")
        appendLine("| Period  | Class".padEnd(max) + "|")
        appendLine("+" + "-".repeat(lengthOfTable - 2) + "+")
        for (period in Period.values()) {
            val courseClass = schedule[period]
            val description = courseClass?.scheduleDescription ?: "Free"
            appendLine("| Period ${period.number}" + "| $description".padEnd(max) + "|")
        }
        appendLine("+" + "-".repeat(lengthOfTable - 2) + "+")
        appendLine("```")
    }

    context(EmbedBuilder) suspend fun createProfileEmbed(student: Student) {
        title = "Profile for ${student.fullNameWithMiddleInitial}"
        val member = guild.member(student)
        // new version with fields
        field {
            name = "Student ID"
            value = student.studentId
        }
        field {
            name = "Birthday"
            value = student.birthday.toFormattedString()
        }
        field {
            name = "Discord"
            value = student.mention
        }
        field {
            name = "Joined"
            value = member.joinedAt.toFormattedString()
        }
        field {
            name = "Joined Eight Lakes"
            value = student.createdAt.toFormattedString()
        }
        footer {
            text = "If you need to want to change any of this information, please contact a staff member."
        }
        image = member.avatar?.url ?: member.defaultAvatar.url
    }

    // createScheduleEmbed
    context (EmbedBuilder) suspend fun createScheduleEmbed(student: Student) {
        title = "Schedule for ${student.fullNameWithMiddleInitial}"
        val schedule = getSchedule(student)
        for (period in Period.values()) {
            val courseClass = schedule[period]
            val description = courseClass?.scheduleDescription ?: "Free"
            field {
                name = "Period ${period.number}"
                value = description
            }
        }
    }

}