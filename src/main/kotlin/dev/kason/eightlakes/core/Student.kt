package dev.kason.eightlakes.core

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.*

object Students : IntIdTable("students") {
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val lastName = varchar("last_name", 255)
    val preferredName = varchar("preferred_name", 255).nullable()
    val studentId = char("student_id", 8)
    val discordId = snowflake("discord_id")
    val birthday = date("birthday")
    val verified = bool("verified")
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)

    var firstName by Students.firstName
    var middleName by Students.middleName
    var lastName by Students.lastName
    var preferredName by Students.preferredName
    var studentId by Students.studentId
    var discordId by Students.discordId
    var birthday by Students.birthday
    var isVerified by Students.verified

    val classes by StudentClass referrersOn StudentClasses.student

    inline val email: String get() = "$studentId@students.katyisd.org"
}

object StudentVerifications : IntIdTable("student_verifications") {
    val student = reference("student", Students)
    val token = char("token", 32).uniqueIndex()
    val email = char("email", 29)
    val expirationTime = timestamp("expiration_time")
}

class StudentVerification(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentVerification>(StudentVerifications)

    var student by Student referencedOn StudentVerifications.student
    var token by StudentVerifications.token
    var email by StudentVerifications.email
    var expirationDate by StudentVerifications.expirationTime

    private var _expiredCache: Boolean = false
    val isExpired: Boolean
        get() {
            if (!_expiredCache) _expiredCache = Clock.System.now() < expirationDate
            return _expiredCache
        }
}