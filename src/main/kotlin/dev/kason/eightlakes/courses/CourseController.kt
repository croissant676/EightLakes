package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordController
import org.kodein.di.DI

class CourseController(override val di: DI) : DiscordController(di) {
    override suspend fun loadCommands() {
        TODO("Not yet implemented")
    }

}
