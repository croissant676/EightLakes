package dev.kason.slhsdb

import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent

typealias CommandExecution = suspend GuildChatInputCommandInteractionCreateEvent.() -> Unit

private val commandMap: MutableMap<ApplicationCommand, CommandExecution> = mutableMapOf()

fun onExecute(commandExecution: CommandExecution = {}) {

}