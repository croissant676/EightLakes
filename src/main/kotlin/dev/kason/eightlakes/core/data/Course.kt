package dev.kason.eightlakes.core.data

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object Courses : IntIdTable("courses") {
    val courseName = varchar("name", 255).index()
    val courseLevel = enumeration<CourseLevel>("level")
    val discordRole = snowflake("role_id")
    val discordChannel = snowflake("channel_id").index()
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var courseName by Courses.courseName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordChannel by Courses.discordChannel
}

enum class CourseLevel { AP, KAP, Academic, Other }