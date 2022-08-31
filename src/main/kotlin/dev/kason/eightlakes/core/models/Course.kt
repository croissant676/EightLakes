package dev.kason.eightlakes.core.models

import dev.kason.eightlakes.core.utils.snowflake
import dev.kason.eightlakes.core.utils.toOrdinal
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Courses : IntIdTable("courses") {
    val courseName = varchar("course_name", 20).index()
    val courseLevel = enumeration<CourseLevel>("course_level")
    val discordRole = snowflake("discord_role")
    val discordChannel = snowflake("discord_channel").nullable()
    val channelEmoji = varchar("emoji", 10)
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var courseName by Courses.courseName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordCourse by Courses.discordChannel
    var channelEmoji by Courses.channelEmoji
    val teachers by TeacherClass referrersOn TeacherClasses.course
}

enum class CourseLevel { KAP, AP, Academic, Other; }

enum class Period {
    First, Second, Third, Fourth, Fifth, Sixth, Seventh;

    val periodNumber: Int get() = ordinal + 1
    val periodName: String get() = (ordinal + 1).toOrdinal()
}

object Classes : IntIdTable("classes") {
    val course = reference("course", Courses)
    val teacher = reference("teacher", Teachers)
    val period = enumeration<Period>("period")
    val roleId = snowflake("discord")
}

class Class(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Class>(Classes)

    var course by Course referencedOn Classes.course
    var teacher by Teacher referencedOn Classes.teacher
    var period by Classes.period
    var roleId by Classes.roleId
}