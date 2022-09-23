package dev.kason.eightlakes.students

import dev.kason.eightlakes.DiscordController
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import mu.KLogging
import org.kodein.di.*

class StudentController(override val di: DI) : DiscordController(di) {
    private val studentService: StudentService by di.instance()
    private val verificationService: VerificationService by di.instance()

    companion object : KLogging()

    override suspend fun loadCommands() {
        chatInputCommand("signup", "Sign up to Eight Lakes") {
            string("first", "Your first name, as it would appear on your schedule", Required)
            string("last", "Your last name, as it would appear on your schedule", Required)
            string("id", "Your student ID") {
                required = true
                minLength = 8
                maxLength = 8
            }
            string("birthday", "Your birthday, in MM/DD/YYYY format") {
                required = true
                minLength = 10
                maxLength = 10
            }
            string("middle", "Your middle name, as it would appear on your schedule", NotRequired)
            string("preferred", "Your preferred name", NotRequired)
        }.onExecute {
            // any database transaction may take a while, so it's better to defer it.
            val response = interaction.deferEphemeralResponse()
            val strings = interaction.command.strings
            val first by strings
            val middle = strings["middle"]
            val preferred = strings["preferred"]
            val last by strings
            val id by strings
            val birthday by strings
            val discordUser = interaction.user
            val student = studentService.signup(
                first,
                middle,
                last,
                preferred,
                id,
                birthday,
                discordUser
            )
            response.respond {
                content = """
                    ${Emojis.whiteCheckMark} You've been signed up! 
                    Check your school email `${student.email}` for a verification code.
                    Then, run `/verify <code>` to verify your account.
                """.trimIndent()
            }
        }
        chatInputCommand("verify", "Verifies your account with a token") {
            string("token", "The token you should have received in your school email.")
        }.onExecute {
            // Only 1 param; we can skip creating a variable map
            val token by interaction.command.strings
            verificationService.close(token, interaction.user)
        }
        chatInputCommand("profile", "Displays the profile of a user") {
            user("user", "The user to display the profile of", NotRequired)
        }.onExecute {
            val user = interaction.command.users["user"] ?: interaction.user
            val student = studentService.get(user.id)
            val response = interaction.deferPublicResponse()
            response.respond {
                embed {
                    studentService.createProfileEmbed(student)
                }
            }
        }
        chatInputCommand("schedule", "Displays the schedule of a user") {
            user("user", "The user to generate the schedule for", NotRequired)
        }.onExecute {
            val user = interaction.command.users["user"] ?: interaction.user
            val student = studentService.get(user.id)
            val response = interaction.deferPublicResponse()
            response.respond {
                embed {
                    studentService.createScheduleEmbed(student)
                }
            }
        }
    }
}
