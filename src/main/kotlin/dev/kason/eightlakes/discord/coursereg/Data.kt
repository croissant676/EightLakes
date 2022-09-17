package dev.kason.eightlakes.discord.coursereg

import dev.kason.eightlakes.core.data.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

// Represents an ongoing registration.
// Registration -> courses being inputted
// signup -> something being created.
object Registrations : IntIdTable("ongoing_registrations") {
    val student = snowflake("student").uniqueIndex()
    val channel = snowflake("channel").uniqueIndex()
    val period1 = reference("p1", Courses).nullable()
    val period2 = reference("p2", Courses).nullable()
    val period3 = reference("p3", Courses).nullable()
    val period4 = reference("p4", Courses).nullable()
    val period5 = reference("p5", Courses).nullable()
    val period6 = reference("p6", Courses).nullable()
    val period7 = reference("p7", Courses).nullable()
    val currentNumber = integer("current_period")
}

class Registration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Registration>(Registrations)

    var student by Registrations.student
    var channel by Registrations.channel
    var period1 by Registrations.period1
    var period2 by Registrations.period2
    var period3 by Registrations.period3
    var period4 by Registrations.period4
    var period5 by Registrations.period5
    var period6 by Registrations.period6
    var period7 by Registrations.period7
    var currentNumber by Registrations.currentNumber
}