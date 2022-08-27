package dev.kason.eightlakes.core.models

import dev.kason.eightlakes.core.utils.toOrdinal
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Courses : IntIdTable("courses") {
    val courseName = varchar("course_name", 20).index()
    val courseLevel = enumeration<CourseLevel>("course_level")

}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

}

enum class CourseLevel {
    KAP, AP, Academic, Other
}

enum class Period {
    First, Second, Third, Fourth, Fifth, Sixth, Seventh;

    val periodNumber: Int get() = ordinal + 1
    val periodName: String get() = (ordinal + 1).toOrdinal()
}