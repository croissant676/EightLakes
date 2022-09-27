package dev.kason.eightlakes.assignments

import dev.kason.eightlakes.DiscordController
import org.kodein.di.DI

class AssignmentController(override val di: DI) : DiscordController(di) {
    override suspend fun loadCommands() {

    }

}