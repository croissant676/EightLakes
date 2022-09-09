package dev.kason.eightlakes.core.data

import dev.kason.eightlakes.core.suspendTransaction
import dev.kason.eightlakes.discord.*
import dev.kord.core.entity.Role
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

enum class CourseLevel {
    AP, KAP, Academic, Other;
}

object Courses : IntIdTable("courses") {
    val courseName = varchar("name", 255).index()
    val courseLevel = enumeration<CourseLevel>("level")
    val discordRole = snowflake("role_id").nullable()
    val discordChannel = snowflake("channel_id").index().nullable()
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var courseName by Courses.courseName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordChannel by Courses.discordChannel
    val classes by CourseClass referrersOn CourseClasses.course

    suspend fun allStudentClasses(): Set<StudentClass> = suspendTransaction {
        this@Course.classes.flatMap { it.students }
    }.toSet()

    suspend fun allStudents(): Set<Student> = suspendTransaction {
        this@Course.classes.flatMap { it.students }.map { it.student }
    }.toSet()

    suspend fun createRole(): Role {
        val students = allStudents()
        val role = role {
            name = courseName
        }
        students.forEach {
            val member = it.member()
            member.addRole(role.id, "Added ${member.displayName} to the class ${courseName}.")
        }
        return role
    }
}