@file:OptIn(PrivilegedIntent::class)

package dev.kason.eightlakes

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.gateway.*
import dev.kord.rest.builder.message.create.actionRow
import io.kotest.core.spec.style.StringSpec
import org.kodein.di.*
import uy.klutter.config.typesafe.loadApplicationConfig

// set up a test
// for discord service
// inject config: Config, application: Application, kord: Kord, guild: Guild
// use kotest
class DiscordServiceTest : StringSpec({

    "test_commands" {
        val config = loadApplicationConfig()
        val modules = setOf(
            EightLakesApp.createModule(config),
            DiscordService.createModule(config)
        )
        val di = DI {
            importAll(modules)
        }
        val discordService: DiscordService by di.instance()
        discordService.controllers += TestableController(di) {
            parentCommand("ping", "This may work..") {
                subCommand("hello", "does this work??") {

                }.onExecute {
                    interaction.respondPublic { content = "sub commands worked" }
                }
                group("pong", "dfdfd") {
                    command("sport", "ping pong is a sport confirmed") {

                    }.onExecute {
                        interaction.respondPublic { content = "nested commands work!" }
                    }
                }
            }
        }
        discordService.init()
        val kord: Kord by di.instance()
        kord.login {
            intents += Intents.all
            presence {
                watching("students suffer")
            }
        }
    }
    "test_subcommnads" {
        val config = loadApplicationConfig()
        val modules = setOf(
            EightLakesApp.createModule(config),
            DiscordService.createModule(config)
        )
        val di = DI {
            importAll(modules)
        }
        val discordService: DiscordService by di.instance()
        discordService.controllers += TestableController(di) {
            parentCommand("ping", "This may work..") {
                subCommand("button", "does this work??").onExecute {
                    var response: PublicMessageInteractionResponseBehavior = interaction.respondPublic {
                        this.actionRow {
                            menu("menu-crap-thingy") {
                                option("option1", "option1") {
                                    description = "option1"
                                }
                                option("option2", "option2") {
                                    description = "option2"
                                }
                            }.onExecute {
                                interaction.respondPublic { content = "menu worked" }
                                this.interaction
                            }
                        }
                    }
                }
            }
        }
        discordService.init()
        val kord: Kord by di.instance()
        kord.login {
            intents += Intents.all
            presence {
                watching("students suffer")
            }
        }
    }
})

class TestableController(override val di: DI, private val block: suspend DiscordController.() -> Unit) :
    DiscordController(di) {
    override suspend fun loadCommands() = block()
}