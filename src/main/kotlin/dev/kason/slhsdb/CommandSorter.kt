package dev.kason.slhsdb

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent

typealias CommandExecution = suspend GuildChatInputCommandInteractionCreateEvent.() -> Unit

private val commandMap: MutableMap<String, CommandExecution> = mutableMapOf()

fun ApplicationCommand.onExecute(commandExecution: CommandExecution = {}) = apply {
    commandMap[name] = commandExecution
}

suspend fun registerCommandSorter() {
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val interaction = this.interaction
        val name = interaction.command.rootName
        commandMap.getOrElse(name) {
            throw IllegalStateException("Command $name could not be found in command sorter")
        }(this)
    }
    kord.createGlobalChatInputCommand("ping", "pong!").onExecute {
        val response = interaction.deferPublicResponse()
        response.respond {
            content = "pong!"
        }
    }
}

suspend fun checkGuildSaveUsed() {
    kord.on<MessageCreateEvent> {
        if (message.author?.isBot) return@on
        if ("guild save" !in message.content) return@on

        // all clear, give them the pong!
        message.channel.createMessage("pong!")
    }

    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}