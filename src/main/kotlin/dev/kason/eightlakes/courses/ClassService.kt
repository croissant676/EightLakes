package dev.kason.eightlakes.courses

import dev.kord.common.entity.Snowflake
import org.kodein.di.*

class ClassService(override val di: DI) : DIAware {

    private val teacherService: TeacherService by instance()
    private val courseService: CourseService by instance()

    suspend fun createCourseClass(
        courseId: Snowflake,
        teacherId: Snowflake,
        periodString: String
    ): CourseClass {
        val period = requireNotNull(Period.parse(periodString)) {
            "Could not parse $periodString"
        }
        val course = courseService.get(courseId)
        val teacher = teacherService.get(teacherId)
        val roleName = "${teacher.lastName}-${course.courseName}-${period.ordinalString}"
        TODO()
    }
}