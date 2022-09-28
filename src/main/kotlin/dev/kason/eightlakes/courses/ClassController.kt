package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kason.eightlakes.courses.Period.Companion.addPeriodValues
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.x.emoji.Emojis
import io.ktor.util.*
import org.kodein.di.*
import kotlin.random.Random

class ClassController(override val di: DI) : DiscordController(di) {

    private val classService: ClassService by instance()

    @Suppress("LABEL_NAME_CLASH")
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
            subCommand("delete", "delete") {
                role("class", "Deletes this class")
            }.onExecute {
                val response = interaction.deferPublicResponse()
                val role = interaction.command.roles["class"]!!
                val courseClass = classService.get(role)
                val discordRole = courseClass.discordRole
                val isSafeToDelete = classService.isSafeToDelete(courseClass)
                if (isSafeToDelete) {
                    response.respond {
                        content = "${Emojis.whiteCheckMark} Deleted class <@&${courseClass.discordRole}>"
                        allowedMentions = AllowedMentionsBuilder()
                    }
                    return@onExecute
                }
                response.respond {
                    content =
                        "${Emojis.warning} Class <@&$discordRole> is not safe to delete. There are students in this class. " +
                                "Are you sure you want to delete this class? (y/n)"
                    allowedMentions = AllowedMentionsBuilder()
                    actionRow {
                        var used = false
                        button(
                            "deleted-class-${courseClass.id.value}-${Random.nextBytes(24).encodeBase64()}",
                            ButtonStyle.Danger
                        ) {
                            label = "Yes"
                        }.onExecute {
                            if (used) {
                                interaction.respondPublic {
                                    content = "${Emojis.x} This button has already been used."
                                }
                                return@onExecute
                            }
                            used = true
                            val newResponse = interaction.deferPublicResponse()
                            classService.delete(courseClass)
                            newResponse.respond {
                                content = "${Emojis.whiteCheckMark} Deleted the class ${courseClass.roleName}."
                            }
                        }
                        button(
                            "keep-class-${courseClass.id.value}-${Random.nextBytes(24).encodeBase64()}",
                            ButtonStyle.Secondary
                        ) {
                            label = "No"
                        }.onExecute {
                            if (used) {
                                interaction.respondPublic {
                                    content = "${Emojis.x} This button has already been used."
                                }
                                return@onExecute
                            }
                            classService.delete(courseClass)
                            used = true
                            interaction.respondPublic {
                                content = "${Emojis.whiteCheckMark} Course ${courseClass.discordRole} was not deleted."
                            }
                        }
                    }
                }
                classService.delete(courseClass)
            }
        }
    }
}