package dev.kason.eightlakes.discord

import dev.kason.eightlakes.utils.ModuleProducer
import org.kodein.di.DI

object DiscordModule : ModuleProducer {
    override fun createModule(): DI.Module = DI.Module(name = "discord_module") {

    }

}