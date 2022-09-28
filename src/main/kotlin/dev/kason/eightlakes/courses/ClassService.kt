package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*
import kotlin.random.Random

// class service is not a controller, just needs controller utilities.
class ClassService(override val di: DI) : DIAware, DiscordEntityService<CourseClass>, DiscordController(di) {

    private val teacherService: TeacherService by instance()
    private val courseService: CourseService by instance()

    override suspend fun loadCommands() {
        //
    }

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
                ?: throw IllegalArgumentException("No class with id <@&$discordId> found")
        }
    }

    override suspend fun getOrNull(discordId: Snowflake): CourseClass? {
        return newSuspendedTransaction {
            CourseClass.find { CourseClasses.discordRole eq discordId }.firstOrNull()
        }
    }

    suspend fun isSafeToDelete(courseClass: CourseClass): Boolean {
        return newSuspendedTransaction {
            StudentClass.find { StudentClasses.courseClass eq courseClass.id }.any()
        }
    }

    suspend fun delete(course: CourseClass) {
        guild.getRole(course.discordRole).delete()
        newSuspendedTransaction {
            course.delete()
        }
    }


    private suspend fun EmbedBuilder.courseDisplayEmbed(page: Int, list: List<Course>) {
        title = "List of Courses"
        newSuspendedTransaction {
            for (item in list) {
                val classes = item.classes.toList()
                field {
                    name = item.courseName
                    value = classes.joinToString("\n") { " - <@&${it.discordRole}>" }.ifEmpty { " - No classes." }
                }
            }
        }
        color = generateRandomColor()
        footer {
            text = "Page ${page + 1} of ${list.size / 10 + 1}"
        }
    }

    private suspend fun fetch(page: Int): List<Course> {
        return newSuspendedTransaction {
            Course.all().limit(10, (page * 10).toLong()).toList()
        }
    }

    suspend fun MessageCreateBuilder.courseDisplayEmbed() {
        courseDisplayEmbed(0, fetch(0))
    }

    private suspend fun MessageCreateBuilder.courseDisplayEmbed(currentPage: Int, list: List<Course>) {
        embed {
            courseDisplayEmbed(currentPage, list)
        }
        actionRow {
            button("move-left-${Random.nextBytes(15).encodeBase64()}") {
                label = "<- Previous"
                disabled = (currentPage == 0)
            }.onExecute {
                val response = interaction.deferPublicMessageUpdate()
                // do some calculations to get embed for currentPage - 1
                val newList = fetch(currentPage - 1)
                response.edit {
                    courseDisplayEmbed(currentPage - 1, newList)
                }
            }
            val rightButtonDisabled =
                (list.size != 10) || (newSuspendedTransaction { CourseClass.all().count() } <= (currentPage + 1) * 10)
            button("move-right-${Random.nextBytes(15).encodeBase64()}") {
                label = "Next ->"
                disabled = rightButtonDisabled
            }.onExecute {
                val response = interaction.deferPublicMessageUpdate()
                val newList = fetch(currentPage + 1)
                response.edit {
                    courseDisplayEmbed(currentPage + 1, newList)
                }
            }
        }
    }


}