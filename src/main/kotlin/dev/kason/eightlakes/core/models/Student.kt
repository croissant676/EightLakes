@file:Suppress("MemberVisibilityCanBePrivate")

package dev.kason.eightlakes.core.models

import dev.kason.eightlakes.core.utils.snowflake
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Students : IntIdTable("students") {
    val firstName = varchar("first_name", 30).index()
    val middleName = varchar("middle_name", 30).nullable()
    val lastName = varchar("last_name", 30).index()
    val studentId = varchar("student_id", 8).uniqueIndex("student_id_unique")
    val preferredName = varchar("preferred_name", 20).nullable().index()
    val birthday = date("birthday").nullable()
    val discordId = snowflake("discord_id").uniqueIndex("student_discord_id_unique")
    val verified = bool("verified")
    val notifTime = duration("global_notif_value").nullable()
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
    var discordId by Students.discordId
    var notifTime by Students.notifTime
    val classesIterable by StudentClass referrersOn StudentClasses.student
    val email: String get() = "$studentId@students.katyisd.org"
    val prefOrFirstName: String get() = preferredName ?: firstName
    var _classesListCache: List<StudentClass>? = null
}

object StudentClasses : IntIdTable("student_classes") {
    val student = reference("student", Students)
    val course = reference("class", Classes)
    val notes = text("notes")
    val notifTime = duration("global_notif_value").nullable()
}

class StudentClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentClass>(StudentClasses)

    var student by Student referencedOn StudentClasses.student
    var `class` by Class referencedOn StudentClasses.course
    var notes by StudentClasses.notes
    var notifTime by StudentClasses.notifTime
    val course: Course get() = `class`.course
}

object StudentVerifications : IntIdTable("student_verification") {
    val studentReference = reference("student", Students)
    val token = varchar("token", 32)
    val expirationDate = timestamp("expiration")
}

class StudentVerification(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentVerification>(StudentVerifications)

    var student by StudentVerifications.studentReference
    var token by StudentVerifications.token
    var expirationDate by StudentVerifications.expirationDate
    private var _cachedExpire = false
    val expired: Boolean
        get() = if (!_cachedExpire) {
            (Clock.System.now() > expirationDate).also { _cachedExpire = it }
        } else true

}
