package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import org.kodein.di.DI

class RegistrationController(override val di: DI) : DiscordController(di) {

    override suspend fun loadCommands() {
        chatInputCommand(
            "register",
            "Register for the course",
        ) {

        }
    }
}