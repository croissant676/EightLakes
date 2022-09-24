package dev.kason.eightlakes.courses

import dev.kason.eightlakes.snowflake
import dev.kason.eightlakes.students.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.ReferenceOption

object CourseClasses : IntIdTable("course_classes") {
    val course = reference("course_id", Courses, onDelete = ReferenceOption.CASCADE)
    val teacher = reference("teacher_id", Teachers, onDelete = ReferenceOption.CASCADE)
    val period = enumeration<Period>("period")
    val discordRole = snowflake("discord_role_id")
}

class CourseClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseClass>(CourseClasses)

    var course by Course referencedOn CourseClasses.course
    var teacher by Teacher referencedOn CourseClasses.teacher
    var period by CourseClasses.period
    var discordRole by CourseClasses.discordRole
}

object StudentClasses : IntIdTable("student_class") {
    val student = reference("student_id", Students, onDelete = ReferenceOption.CASCADE)
    val courseClass = reference("course_class_id", CourseClasses, onDelete = ReferenceOption.CASCADE)
    val notes = text("notes").nullable()
    val notification = bool("notification").default(false)
}

class StudentClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentClass>(StudentClasses)

    var student by Student referencedOn StudentClasses.student
    var courseClass by CourseClass referencedOn StudentClasses.courseClass
    var notes by StudentClasses.notes
    var notification by StudentClasses.notification
}

// Utils

val CourseClass.scheduleDescription: String
    get() = "${course.courseName} - ${teacher.fullName}"