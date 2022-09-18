package dev.kason.eightlakes.students

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object StudentVerifications : IntIdTable("student_verifications") {
    val student = reference("student", Students)
    val token = char("token", 32)
    val email = char("email", 29)
}

class StudentVerification(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentVerification>(StudentVerifications)

}