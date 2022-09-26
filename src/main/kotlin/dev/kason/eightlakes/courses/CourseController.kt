package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import dev.kason.eightlakes.courses.CourseLevel.Companion.addCourseLevelValues
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.Category
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.x.emoji.Emojis
import org.kodein.di.*

class CourseController(override val di: DI) : DiscordController(di) {
    private val courseService: CourseService by instance()
    override suspend fun loadCommands() {
        parentCommand("courses", "Edit, create, or modify class entities") {
            subCommand("new", "Register a course") {
                string(
                    "name",
                    "The full name of the course. For instance: `AP Statistics` or `KAP Precalculus`",
                    Required
                )
                string(
                    "simple",
                    "A simplified, more easy to type name for the course. For instance: `chem` or `stats`. ",
                    Required
                )
                role("role", "The role to assign to students in this course", NotRequired)
                channel("channel", "The channel to assign to this course", NotRequired)
                channel(
                    "category",
                    "The category that the channel will be created in, if no channel has been specified.",
                    NotRequired
                )
                string(
                    "level",
                    "The level of the course. For instance: `AP`, `KAP`."
                ) {
                    addCourseLevelValues()
                }
            }.onExecute {
                val response = interaction.deferPublicResponse()
                val strings = interaction.command.strings
                val role = interaction.command.roles["role"]
                val channel = interaction.command.channels["channel"]
                val nullableLevel =
                    interaction.command.strings["level"]?.toIntOrNull()?.let { CourseLevel.values().getOrNull(it) }
                val name by strings
                val simple by strings
                val category = interaction.command.channels["category"]?.asChannelOf<Category>()
                val course = courseService.registerCourse(
                    name,
                    simple,
                    role?.id,
                    channel?.id,
                    nullableLevel,
                    category
                )
                response.respond {
                    content =
                        "${Emojis.whiteCheckMark} Successfully registered course `${course.courseName}` (level: ${course.courseLevel}). Role: <@&${course.discordRole}>, channel: <#${course.discordChannel}>"
                    allowedMentions = AllowedMentionsBuilder()
                }
            }
        }
    }

}
