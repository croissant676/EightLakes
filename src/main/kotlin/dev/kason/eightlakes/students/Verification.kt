package dev.kason.eightlakes.students

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object StudentVerifications : IntIdTable("student_verifications") {
    val student = reference("student", Students)

    // Don't expose pk
    val token = char("token", 32).uniqueIndex()
    val email = char("email", 29)
    val expirationDate = timestamp("expiration_date")
}

class StudentVerification(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentVerification>(StudentVerifications)

    var student by Student referencedOn StudentVerifications.student
    var token by StudentVerifications.token
    var email by StudentVerifications.email
    var expirationDate by StudentVerifications.expirationDate
}