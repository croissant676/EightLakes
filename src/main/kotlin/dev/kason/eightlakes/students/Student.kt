package dev.kason.eightlakes.students

import dev.kason.eightlakes.utils.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.kodein.di.*

object Students : IntIdTable("students") {
    val firstName = varchar("first_name", 255)
    val middleName = varchar("first_name", 255).nullable()
    val lastName = varchar("first_name", 255)
    val preferredName = varchar("first_name", 255).nullable()
    val studentId = char("student_id", 8)
    val discordId = snowflake("discord_id").uniqueIndex()
    val birthday = date("birthday")
    val verified = bool("verified").default(false)
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students), ModuleProducer {
        override fun createModule(): DI.Module = DI.Module {
            bindSingleton { StudentService(di) }
            bindSingleton { StudentController(di) }
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
}