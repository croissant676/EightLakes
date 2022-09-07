package dev.kason.eightlakes.core.data

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object Teachers : IntIdTable("teachers") {
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val lastName = varchar("last_name", 255)
    val preferredName = varchar("preferred_name", 255).nullable()
    val email = varchar("email", 255)
}

class Teacher(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Teacher>(Teachers)

    var firstName by Teachers.firstName
    var middleName by Teachers.middleName
    var lastName by Teachers.lastName
    var preferredName by Teachers.preferredName
    var email by Teachers.email
}

