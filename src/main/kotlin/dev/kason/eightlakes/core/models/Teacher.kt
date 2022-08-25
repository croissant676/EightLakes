package dev.kason.eightlakes.core.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object Teachers : IntIdTable("teachers") {
    val firstName = varchar("first_name", 30).index()
    val middleName = varchar("middle_name", 30).nullable()
    val lastName = varchar("last_name", 30).index()
    val email = varchar("teacher_id", 40)
}

class Teacher(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Teacher>(Students)

    var firstName by Teachers.firstName
    var middleName by Teachers.middleName
    var lastName by Teachers.lastName
    var email by Teachers.email
}
