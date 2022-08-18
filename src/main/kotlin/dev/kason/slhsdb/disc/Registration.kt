package dev.kason.slhsdb.disc

import dev.kason.slhsdb.core.StudentRegistrationError
import dev.kason.slhsdb.core.registerStudent
import dev.kason.slhsdb.kord
import dev.kason.slhsdb.onExecute
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.flow.collectLatest

suspend fun addRegistrationCommand() = kord.guilds.collectLatest {
    kord.createGuildChatInputCommand(
        it.id,
        "signup",
        "Registers you into the system with the given information. You can edit the information later."
    ) {
        string("id", "Your student ID. Please capitalize the first letter") {
            required = true
        }
        string("first", "Your first name as it would appear on your schedule") {
            required = true
        }
        string("last", "Your last name as it would appear on your schedule") {
            required = true
        }
        string("middle", "Your middle name, if you have one") {
            required = false
        }
        string("preferred", "Your preferred name, if you have one") {
            required = false
        }
    }.onExecute {
        val response = this.interaction.deferEphemeralResponse()
        val command = interaction.command
        val id = command.strings["id"]!!
        val first = command.strings["first"]!!
        val last = command.strings["last"]!!
        val middle = command.strings["middle"]
        val preferred = command.strings["preferred"]
        registerStudent(
            interaction.user.id,
            first,
            middle,
            last,
            id,
            preferred
        ).fold({ studentRegistrationError ->
            response.respond {
                content = when (studentRegistrationError) {
                    StudentRegistrationError.AccountRegistered -> "You are already registered!"
                    StudentRegistrationError.StudentNameAlreadyExists -> "A student with the same first, middle, and last name as you has already exists."
                    StudentRegistrationError.StudentCodeInvalid -> "The inputted student code is invalid."
                    StudentRegistrationError.StudentCodeAlreadyExists -> "A student with the same student id is already registered."
                }
            }
        }, {
            response.respond {
                content =
                    "Sent the verification token, ${interaction.user.mention}. You should get an email to your KatyISD email. Be sure to check the 'Spam' folder"
            }
        })
    }
}