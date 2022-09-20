package dev.kason.eightlakes.students

import dev.kason.eightlakes.EightLakesApp
import dev.kason.eightlakes.utils.*
import freemarker.template.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
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
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students), ModuleProducer {
        override fun createModule(): DI.Module = DI.Module(name = "student_module") {
            bindSingleton { StudentService(di) }
            bindSingleton { StudentController(di) }
            bindSingleton { VerificationService(di) }
            bindSingleton { createConfiguration() }
        }

        private fun createConfiguration() = Configuration(Version(2, 3, 31)).apply {
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

    val email: String get() = "$studentId@students.katyisd.org"
    val preferredOrFirst: String get() = preferredName ?: firstName
}