package dev.kason.eightlakes.courses

import dev.kason.eightlakes.snowflake
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object Courses : IntIdTable("courses") {
    val courseName = varchar("course_name", 255)
    val courseLevel = enumeration<CourseLevel>("course_level")
    val discordRole = snowflake("discord_role")
    val discordChannel = snowflake("discord_channel")
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var courseName by Courses.courseName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordChannel by Courses.discordChannel
}