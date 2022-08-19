package dev.kason.slhsdb.disc

import dev.kason.slhsdb.core.StudentRegistrationError
import dev.kason.slhsdb.core.finish
import dev.kason.slhsdb.core.registerStudent
import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.kord
import dev.kason.slhsdb.onExecute
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.flow.toList

suspend fun addRegistrationCommand() {
    kord.createGuildChatInputCommand(
        guildId,
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
                    "Thanks for registering, ${interaction.user.mention}! Now, check your Katyisd email and run the command /verify {token} in order to complete the registration."
            }
        })
    }
}

suspend fun addVerificationCommand() {
    kord.createGuildChatInputCommand(
        guildId,
        "verify",
        "Verifies your signup with the following verification token."
    ) {
        string("token", "The token that you received.") {
            required = true
        }
    }.onExecute {
        val user = interaction.user.id
        val token = interaction.command.strings["token"]!!
        val result = finish(token, user)
        val response = interaction.deferEphemeralResponse()
        result.tapLeft {
            response.respond {
                content = it
            }
        }.tap {
            response.respond {
                content = "Thanks for verifying. You've been signed up! :white_check_mark:"
                interaction.user.addRole(_role!!, "Verification")
            }
        }
    }
}

private var _role: Snowflake? = null

val pobrecitosRoleId: Snowflake get() = _role!!

private const val searchRoleName: String = "Los Pobrecitos"

suspend fun addVerifiedRole() {
    val guild = kord.getGuild(guildId)!!
    for (currentRole in guild.roles.toList()) {
        if (currentRole.name == searchRoleName) {
            _role = currentRole.id
            break
        }
    }
    if (_role == null) _role = guild.createRole {
        name = searchRoleName
        color = Color(52, 219, 168)
    }.id
}