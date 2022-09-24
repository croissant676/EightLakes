package dev.kason.eightlakes.courses

import dev.kason.eightlakes.snowflake
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object Teachers : IntIdTable() {
    val firstName = varchar("first_name", 255)
    val middleInitial = char("middle_initial").nullable()
    val lastName = varchar("last_name", 255)
    val email = varchar("email", 255)
    val role = snowflake("role").nullable().uniqueIndex()
}

class Teacher(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Teacher>(Teachers)

    var firstName by Teachers.firstName
    var middleInitial by Teachers.middleInitial
    var lastName by Teachers.lastName
    var email by Teachers.email
    var role by Teachers.role

    val courseClasses by CourseClass referrersOn CourseClasses.teacher
}

val Teacher.fullName: String
    get() = if (middleInitial != null) {
        "$firstName $middleInitial. $lastName"
    } else {
        "$firstName $lastName"
    }

val Teacher.roleMention: String
    get() = "<@&${role}>"