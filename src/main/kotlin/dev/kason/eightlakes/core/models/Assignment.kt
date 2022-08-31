package dev.kason.eightlakes.core.models

import dev.kason.eightlakes.core.utils.snowflake
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Assignments : IntIdTable("assignments") {
    val classId = reference("class_reference", Classes).nullable()
    val courseId = reference("course_reference", Courses).nullable()
    val dueDate = datetime("due_date")
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val thread = snowflake("thread").nullable()
    val classSpecific = bool("class_specific")
    val isAssessment = bool("assessment")
}

class Assignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Assignment>(Assignments)

    var classId by Assignments.classId
    var courseId by Assignments.courseId
    var dueDate by Assignments.dueDate
    var name by Assignments.name
    var description by Assignments.description
    var thread by Assignments.thread
    var classSpecific by Assignments.classSpecific
    var isAssessment by Assignments.isAssessment
}

object StudentAssignments : IntIdTable("student_assignment") {
    val notes = text("notes").nullable()
    val finished = bool("finished")
    val student = reference("student", Students)
    val assignment = reference("assignment", Assignments)
    val grade = double("grade").nullable()
}

class StudentAssignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentAssignment>(StudentAssignments)

    var notes by StudentAssignments.notes
    var finished by StudentAssignments.finished
    var student by Student referencedOn StudentAssignments.student
    var assignment by Assignment referencedOn StudentAssignments.assignment
    var grade by StudentAssignments.grade
}