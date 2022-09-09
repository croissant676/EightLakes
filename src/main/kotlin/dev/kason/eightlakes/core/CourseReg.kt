package dev.kason.eightlakes.core

import dev.kason.eightlakes.core.data.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// Ensure the person issuing the command has the permission to do so
// before calling this function.
suspend fun registerCourse(
    _fullName: String,
    _courseLevel: String?,
    createRole: Boolean,
    createChannel: Boolean
): Course {
    val lowercase = _fullName.lowercase()
    val courseLevel = when {
        _courseLevel != null -> CourseLevel.valueOf(_courseLevel)
        "kap" in lowercase -> CourseLevel.KAP
        "aca" in lowercase -> CourseLevel.Academic
        "ap" in lowercase -> CourseLevel.AP
        else -> CourseLevel.Other
    }
    return newSuspendedTransaction {
        Course.new {
            this.courseName = _fullName.capitalizeWords()
            this.courseLevel = courseLevel
        }
    }
}

private suspend fun createChannel(course: Course) {

}

