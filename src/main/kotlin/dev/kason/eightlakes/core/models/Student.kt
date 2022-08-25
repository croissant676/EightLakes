@file:Suppress("MemberVisibilityCanBePrivate")

package dev.kason.eightlakes.core.models

import dev.kason.eightlakes.core.snowflakeDelegate
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.date

object Students : IntIdTable("students") {
    val firstName = varchar("first_name", 30).index()
    val middleName = varchar("middle_name", 30).nullable()
    val lastName = varchar("last_name", 30).index()
    val studentId = varchar("student_id", 8).uniqueIndex("student_id_unique")
    val preferredName = varchar("preferred_name", 20).nullable().index()
    val birthday = date("birthday").nullable()
    val discordSnowflakeValue = long("discord_id").index()
    val verified = bool("verified")
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)

    var firstName by Students.firstName
    var middleName by Students.middleName
    var lastName by Students.lastName
    var studentId by Students.studentId
    var preferredName by Students.preferredName
    var birthday by Students.birthday
    var verified by Students.verified
    val classes by StudentClass referrersOn StudentClasses.student

    var discordId by snowflakeDelegate(Students.discordSnowflakeValue)
    val email: String get() = "$studentId@students.katyisd.org"

}

object StudentClasses : IntIdTable("student_classes") {
    val student = reference("student", Students)
    val course = reference("course", Courses)
    val teacher = reference("teacher", Teachers)
    val period = integer("period")
}

class StudentClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentClass>(StudentClasses)

    var student by Student referencedOn StudentClasses.student
    var course by Course referencedOn StudentClasses.course
    var teacher by Teacher referencedOn StudentClasses.teacher
}

object StudentVerifications : IntIdTable("student_verification") {
    val studentReference = reference("student", Students)
    val token = varchar("token", 32)
}

class StudentVerification {

}
