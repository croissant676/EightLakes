package dev.kason.eightlakes.core.data

import dev.kason.eightlakes.core.englishOrdinal
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

// Although in the database it's called a class, it'll be referenced in code as a course class
// to avoid interfering with the java.lang.Class or the class keyword.
object CourseClasses : IntIdTable("class") {
    val course = reference("course", Courses)
    val teacher = reference("teacher", Teachers)
    val period = enumeration<Period>("period")
    val discordRole = snowflake("discord_role")
}

class CourseClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseClass>(CourseClasses)

    var course by Course referencedOn CourseClasses.course
    var teacher by Teacher referencedOn CourseClasses.teacher
    var period by CourseClasses.period
    var discordRole by CourseClasses.discordRole
    val students by StudentClass referrersOn StudentClasses.courseClass
}

object StudentClasses : IntIdTable("student_classes") {
    val courseClass = reference("course_class", CourseClasses)
    val student = reference("student", Students)
    val notes = text("notes", eagerLoading = true)
}

class StudentClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentClass>(StudentClasses)

    var courseClass by CourseClass referencedOn StudentClasses.courseClass
    var student by Student referencedOn StudentClasses.student
    var notes by StudentClasses.notes
}

enum class Period {
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh;

    val periodNumber inline get() = ordinal + 1
    val periodOrdinal inline get() = periodNumber.englishOrdinal()
}

private val periodNames =
    Period.values().associateBy { it.name.lowercase() } +
            Period.values().associateBy { it.periodOrdinal }

fun periodFromString(periodString: String): Period? {
    val lowercaseString = periodString.lowercase()
    if (lowercaseString == "last") return Period.Seventh
    return periodNames[lowercaseString]
}
