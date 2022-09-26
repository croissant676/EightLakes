package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class ClassService(override val di: DI) : DIAware, DiscordEntityService<CourseClass> {

    private val teacherService: TeacherService by instance()
    private val courseService: CourseService by instance()

    private val guild: Guild by instance()

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
        val roleName = "${teacher.lastName}-${course.simpleName}-${period.ordinalString}"
        val role = guild.role {
            name = roleName
            color = generateRandomColor()
        }
        return newSuspendedTransaction {
            CourseClass.new {
                this.course = course
                this.teacher = teacher
                this.period = period
                this.discordRole = role.id
            }
        }
    }

    override suspend fun get(discordId: Snowflake): CourseClass {
        return newSuspendedTransaction {
            CourseClass.find { CourseClasses.discordRole eq discordId }.firstOrNull()
                ?: throw IllegalArgumentException("No class with id $discordId")
        }
    }

    override suspend fun getOrNull(discordId: Snowflake): CourseClass? {
        return newSuspendedTransaction {
            CourseClass.find { CourseClasses.discordRole eq discordId }.firstOrNull()
        }
    }
}