package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.core.entity.application.*
import kotlinx.coroutines.flow.toList

// A simple command cache that stores commands from this bot
// Because Kord doesn't support caching commands, which leads to a bunch of
// REST API calls.
@Suppress("MemberVisibilityCanBePrivate")
object CommandCache {

    // TODO use set
    private val commandMap = Array(3) {
        mutableListOf<GuildApplicationCommand>()
    }

    fun all() = commandMap.flatMap { it }

    fun <T : GuildApplicationCommand> insert(value: T): T {
        val list = retrieveOfType<T>(value.type) as MutableList<T>
        list += value
        return value
    }

    fun removeUnknownBotCommands() {
        for (index in commandMap.indices) {
            val value = commandMap[index]
            commandMap[index] = value.filter {
                it.applicationId == appId && it.type.value - 1 == index
            }.toMutableList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> retrieveOfType(applicationType: ApplicationCommandType): List<T> =
        commandMap[applicationType.value - 1] as List<T>

    suspend fun retrieveCommands() {
        val list = guild.getApplicationCommands().toList()
        list.forEach { insert(it) }
        logger.debug { "Retrieved commands from kord: ${list.map { "${it.name}(${it.guildId})" }}" }
        removeUnknownBotCommands()
    }

    fun clear() = commandMap.forEach { it.clear() }

}

inline fun <reified T : GuildApplicationCommand> CommandCache.retrieveOfType(): List<T> {
    val type = when (val kClass = T::class) {
        GuildChatInputCommand::class -> ApplicationCommandType.ChatInput
        GuildMessageCommand::class -> ApplicationCommandType.Message
        GuildUserCommand::class -> ApplicationCommandType.User
        else -> error("Unknown type <$kClass>")
    }
    return retrieveOfType(type)
}