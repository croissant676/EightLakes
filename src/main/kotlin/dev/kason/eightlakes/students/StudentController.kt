package dev.kason.eightlakes.students

import dev.kason.eightlakes.discord.DiscordController
import dev.kord.rest.builder.interaction.string
import org.kodein.di.*

class StudentController(override val di: DI) : DiscordController(di) {
    private val studentService: StudentService by di.instance()
    private val verificationService: VerificationService by di.instance()

    override suspend fun loadCommands() {
        chatInputCommand("signup", "Sign up to Eight Lakes") {
            string("first", "Your first name, as it would appear on your schedule", Required)
        }.onExecute {
            val strings = interaction.command.strings
            val first by strings

        }
        chatInputCommand("verify", "Verifies your account with a token") {
            string("token", "The token you should have received in your school email.")
        }.onExecute {
            // Only 1 param; we can skip creating a variable map
            val token by interaction.command.strings
            verificationService.close(token, interaction.user)
        }
    }
}
