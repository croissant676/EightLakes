package dev.kason.eightlakes.assignments

import dev.kason.eightlakes.courses.*
import dev.kason.eightlakes.snowflake
import dev.kason.eightlakes.students.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Assignments : IntIdTable("assignments") {
    val course = reference("course_id", Courses)
    val dueDate = datetime("due_date").nullable()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val threadId = snowflake("thread_id").nullable()
    val assessment = bool("assessment").default(false)
}

class Assignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Assignment>(Assignments)

    var course by Course referencedOn Assignments.course
    var dueDate by Assignments.dueDate
    var name by Assignments.name
    var description by Assignments.description
    var threadId by Assignments.threadId
    var isAssessment by Assignments.assessment
}

object StudentAssignments : IntIdTable("student_assignment") {
    val student = reference("student_id", Students)
    val assignment = reference("assignment_id", Assignments)
    val notes = text("notes").nullable()
    val grade = double("grade").nullable()
    val submitted = bool("submitted").default(false)
}

class StudentAssignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentAssignment>(StudentAssignments)

    var student by Student referencedOn StudentAssignments.student
    var assignment by Assignment referencedOn StudentAssignments.assignment
    var notes by StudentAssignments.notes
    var grade by StudentAssignments.grade
    var isSubmitted by StudentAssignments.submitted
}