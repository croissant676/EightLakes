package dev.kason.eightlakes.students

import com.typesafe.config.Config
import dev.kason.eightlakes.*
import dev.kason.eightlakes.utils.*
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import freemarker.template.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import org.kodein.di.*
import java.util.*

object Students : IntIdTable("students") {
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val lastName = varchar("last_name", 255)
    val preferredName = varchar("preferred_name", 255).nullable()
    val studentId = char("student_id", 8)
    val discordId = snowflake("discord_id").uniqueIndex()
    val birthday = date("birthday")
    val verified = bool("verified").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)

    object Loader : ModuleProducer {
        override suspend fun createModule(config: Config): DI.Module = DI.Module(name = "student_module") {
            bindSingleton { StudentService(di) }
            bindSingleton { VerificationService(di) }
            bindSingleton { createFreemarkerConfiguration() }
            bindEagerSingleton { StudentController(di).also { di.direct.instance<DiscordService>().controllers += (it) } }
        }

        private fun createFreemarkerConfiguration() =
            Configuration(Version(2, 3, 31)).apply {
                setClassForTemplateLoading(EightLakesApp::class.java, "/dev/kason/eightlakes/students/templates/")
                defaultEncoding = "UTF-8"
                locale = Locale.getDefault()
            }
    }

    var firstName by Students.firstName
    var middleName by Students.middleName
    var lastName by Students.lastName
    var preferredName by Students.preferredName
    var studentId by Students.studentId
    var discordId by Students.discordId
    var birthday by Students.birthday
    var isVerified by Students.verified
    var createdAt by Students.createdAt

}


val Student.email: String get() = "$studentId@students.katyisd.org"
val Student.preferredOrFirst: String get() = preferredName ?: firstName
val Student.fullName: String
    get() = listOfNotNull(firstName, middleName, lastName).joinToString(" ")
val Student.fullNameWithMiddleInitial: String
    get() = listOfNotNull(firstName, middleName?.let { "${it.first()}." }, lastName).joinToString(" ")

// add preferred with parentheses if it exists
val Student.fullNameWithPreferred: String
    get() = fullName + if (preferredName != null) " ($preferredName)" else ""

suspend fun Kord.user(student: Student) = getUser(student.discordId)
suspend fun Guild.member(student: Student) = getMember(student.discordId)

val Student.mention: String get() = "<@!$discordId>"
