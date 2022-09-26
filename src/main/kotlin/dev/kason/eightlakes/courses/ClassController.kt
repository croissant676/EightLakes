package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import dev.kason.eightlakes.courses.Period.Companion.addPeriodValues
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.x.emoji.Emojis
import org.kodein.di.*

class ClassController(override val di: DI) : DiscordController(di) {

    private val classService: ClassService by instance()

    override suspend fun loadCommands() {
        parentCommand("classes", "Edit, create, or modify class entities") {
            subCommand("new", "Creates a new class.") {
                channel("course", "The channel representing the course", Required)
                role("teacher", "The role representing the teacher", Required)
                string("period", "The period of the class") {
                    required = true
                    addPeriodValues()
                }
            }.onExecute {
                val response = interaction.deferPublicResponse()
                val resolvedChannel = interaction.command.channels["course"]!!
                val teacherRole = interaction.command.roles["teacher"]!!
                val courseClass = classService.createCourseClass(
                    resolvedChannel.id,
                    teacherRole.id,
                    interaction.command.strings["period"]!!
                )
                response.respond {
                    content = "${Emojis.whiteCheckMark} Created class <@&${courseClass.discordRole}>"
                    allowedMentions = AllowedMentionsBuilder()
                }
            }
        }
    }
}