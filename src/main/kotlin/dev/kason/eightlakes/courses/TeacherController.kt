package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.x.emoji.Emojis
import io.ktor.util.*
import org.kodein.di.*
import kotlin.random.Random

class TeacherController(override val di: DI) : DiscordController(di) {

    private val teacherService: TeacherService by instance()

    @Suppress("LABEL_NAME_CLASH")
    override suspend fun loadCommands() {
        parentCommand("teachers", "Edit, create, or modify teacher entities") {
            disableCommandInGuilds()
            subCommand("register", "Registers a new teacher into the database.") {
                string("email", "The email of the teacher.", Required)
                string("first_name", "The first name of the teacher.", Required)
                string("last_name", "The last name of the teacher.", Required)
                string("middle_initial", "The middle initial of the teacher.", NotRequired)
            }.onExecute {
                val response = interaction.deferPublicResponse()
                val strings = interaction.command.strings
                val email by strings
                val firstName = strings["first_name"]!!
                val lastName = strings["last_name"]!!
                val middleInitial = strings["middle_initial"]
                val teacher = teacherService.createTeacher(firstName, middleInitial, lastName, email)
                response.respond {
                    content =
                        "${Emojis.whiteCheckMark} Registered teacher ${teacher.fullName} with role ${teacher.roleMention}."
                }
            }
            subCommand("delete", "Removes that teacher registration from the database") {
                role("teacher", "The teacher to remove from the database.", Required)
            }.onExecute {
                val response = interaction.deferPublicResponse()
                val teacherRole = interaction.command.roles["teacher"]!!
                val teacher = teacherService.get(teacherRole.id)
                if (teacherService.isSafeToDelete(teacher)) {
                    teacherService.deleteTeacher(teacher)
                    response.respond {
                        content = "${Emojis.whiteCheckMark} Deleted teacher ${teacher.fullName}."
                    }
                    return@onExecute
                }
                // make sure that they actually want to delete it:
                // there are course classes and students associated with this teacher
                response.respond {
                    content =
                        "${Emojis.x} There are course classes and students associated with this teacher. Are you sure you want to delete it?"
                    actionRow {
                        var used = false
                        button(
                            "deleted-teacher-${teacher.id.value}-${Random.nextBytes(24).encodeBase64()}",
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
                            teacherService.deleteTeacher(teacher)
                            newResponse.respond {
                                content = "${Emojis.whiteCheckMark} Deleted teacher ${teacher.fullName}."
                            }
                        }
                        button(
                            "keep-teacher-${teacher.id.value}-${Random.nextBytes(24).encodeBase64()}",
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
                            used = true
                            interaction.respondPublic {
                                content = "${Emojis.whiteCheckMark} Teacher ${teacher.fullName} was not deleted."
                            }
                        }
                    }
                }
            }
        }
    }
}