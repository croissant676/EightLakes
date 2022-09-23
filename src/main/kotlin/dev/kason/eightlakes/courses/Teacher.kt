package dev.kason.eightlakes.courses

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object Teachers : IntIdTable() {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
}

class Teacher(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Teacher>(Teachers)

    var name by Teachers.name
    var email by Teachers.email
}