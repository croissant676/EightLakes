package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class RegistrationController(override val di: DI) : DiscordController(di) {

    private val registrationService: RegistrationService by instance()

    override suspend fun loadCommands() {
        chatInputCommand(
            "register",
            "Register your courses",
        ) {
        }.onExecute {
            val response = interaction.deferEphemeralResponse()
            val registration = registrationService.createRegistration(interaction.user)
            response.respond {
                content = "${Emojis.whiteCheckMark} Registration created in <#${registration.channel}>"
            }
        }
        chatInputCommand(
            "add-class",
            "Adds the class to your registration",
        ) {
            role("class", "The class to add", Required)
        }.onExecute {
            registrationService.ensureCorrectChannel(interaction.channel, interaction.user)
            val response = interaction.deferEphemeralResponse()
            val roleClass = interaction.command.roles["class"]!!
            val registration = registrationService.addCourse(interaction.user, roleClass.id)
            if (registration.isFinished) {
                response.respond {
                    content = "${Emojis.whiteCheckMark} Registration completed! ${interaction.user.mention}"
                }
                delay(2000)
                interaction.channel.delete("Registration completed")
                return@onExecute
            }
            response.respond {
                content =
                    "${Emojis.whiteCheckMark} Added ${roleClass.mention} to your registration. Your next period is ${registration.period.ordinalString} period."
                allowedMentions = AllowedMentionsBuilder()
            }
        }
        parentCommand("registrations", "Edit, create, or modify registration entities") {
            subCommand("pause", "Pauses the given registration.") {
                channel("registration", "The registration to pause", Required)
            }.onExecute {
                val response = interaction.deferEphemeralResponse()
                val registration = registrationService.get(interaction.command.channels["registration"]!!.id)
                newSuspendedTransaction {
                    registration.isPaused = true
                }
                response.respond {
                    content = "${Emojis.whiteCheckMark} Registration paused."
                }
            }
            subCommand("close", "Closes the given registration.") {
                channel("registration", "The registration to close", Required)
            }.onExecute {
                val response = interaction.deferEphemeralResponse()
                val registration = registrationService.get(interaction.command.channels["registration"]!!.id)
                newSuspendedTransaction {
                    registrationService.deleteRegistration(registration, registration.student.discordId)
                }
                response.respond {
                    content = "${Emojis.whiteCheckMark} Registration closed."
                }
            }
        }
    }
}