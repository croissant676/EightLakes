package dev.kason.eightlakes.core.data

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Assignments : IntIdTable("assignments") {
    val courseReference = reference("course", Courses)
    val dueDate = timestamp("time")
    val name = varchar("name", 255).index()
    val description = text("description", eagerLoading = true)
    val threadId = snowflake("thread_id").nullable().index()
    val assessment = bool("assessment")
}

class Assignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Assignment>(Assignments)

    var courseReference by Course referencedOn Assignments.courseReference
    var dueDate by Assignments.dueDate
    var name by Assignments.name
    var description by Assignments.description
    var threadId by Assignments.threadId
    var isAssessment by Assignments.assessment
}

object StudentAssignments : IntIdTable("student_assignments") {
    val notes = text("notes", eagerLoading = true)
    val finished = bool("finished")
    val student = reference("student", Students)
    val assignment = reference("assignment", Assignments).index()
    val grade = double("grade")
}

class StudentAssignment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentAssignment>(StudentAssignments)

    var notes by StudentAssignments.notes
    var isFinished by StudentAssignments.finished
    var student by Student referencedOn StudentAssignments.student
    var assignmentId by Assignment referencedOn StudentAssignments.assignment
    var grade by StudentAssignments.grade
}