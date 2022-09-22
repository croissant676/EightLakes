@file:OptIn(PrivilegedIntent::class)

package dev.kason.eightlakes

import dev.kason.eightlakes.discord.*
import dev.kord.core.Kord
import dev.kord.gateway.*
import io.kotest.core.spec.style.StringSpec
import org.kodein.di.*

// set up a test
// for discord service
// inject config: Config, application: Application, kord: Kord, guild: Guild
// use kotest
class DiscordServiceTest : StringSpec({
    beforeAny {
        val kord: Kord by di.instance()
        kord.login {
            intents += Intents.all
            presence {
                watching("students suffer")
            }
        }
    }
    "test_commands" {
        val modules = setOf(
            EightLakesApp.createModule(),
            DiscordService.createModule()
        )
        val di = DI {
            importAll(modules)
        }
        val discordService: DiscordService by di.instance()
        discordService.init()
        val testableController = TestableController(di) {

        }
        testableController.loadCommands()
    }
    "test_subcommnads" {

    }
})

class TestableController(override val di: DI, val block: suspend DiscordController.() -> Unit) : DiscordController(di) {
    override suspend fun loadCommands() = block()
}