package dev.kason.eightlakes.courses

import dev.kason.eightlakes.utils.snowflake
import org.jetbrains.exposed.dao.id.IntIdTable

object Courses : IntIdTable("courses") {
    val courseName = varchar("course_name", 255)
    val courseLevel = enumeration<CourseLevel>("course_level")
    val discordRole = snowflake("discord_role")
    val discordChannel = snowflake("discord_channel")
}

class Course {

}