package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kord.common.entity.*
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.application.*
import dev.kord.core.event.interaction.*
import dev.kord.rest.builder.interaction.*
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import kotlin.reflect.KClass

typealias GuildChatInputEvent = GuildChatInputCommandInteractionCreateEvent
typealias GuildUserCommandEvent = GuildUserCommandInteractionCreateEvent
typealias GuildMessageCommandEvent = GuildMessageCommandInteractionCreateEvent
typealias GuildAppCommandEvent = GuildApplicationCommandInteractionCreateEvent
private typealias ExecutionMap<E> = HashMap<Snowflake, DebuggableExecution<E>>
private typealias CommandExecution<T> = suspend T.() -> Unit

private val commandLogger = KotlinLogging.logger {}

suspend fun chatInputCommand(
    name: String,
    description: String,
    block: ChatInputCreateBuilder.() -> Unit
): GuildChatInputCommand = guild.getApplicationCommands().filter {
    it.data.applicationId == kord.selfId && it.type == ApplicationCommandType.ChatInput && it.name == name
}.firstOrNull() as? GuildChatInputCommand ?: kord.createGuildChatInputCommand(guildId, name, description, block)

suspend fun messageCommand(
    name: String,
    block: MessageCommandCreateBuilder.() -> Unit
): GuildMessageCommand = guild.getApplicationCommands().filter {
    it.data.applicationId == kord.selfId && it.type == ApplicationCommandType.Message && it.name == name
}.firstOrNull() as? GuildMessageCommand ?: kord.createGuildMessageCommand(guildId, name, block)

suspend fun userCommand(
    name: String,
    block: UserCommandCreateBuilder.() -> Unit
): GuildUserCommand = guild.getApplicationCommands().filter {
    it.data.applicationId == kord.selfId && it.type == ApplicationCommandType.User && it.name == name
}.firstOrNull() as? GuildUserCommand ?: kord.createGuildUserCommand(guildId, name, block)

private val chatInputExecutions = ExecutionMap<GuildChatInputEvent>()
private val messageCommandExecutions = ExecutionMap<GuildMessageCommandEvent>()
private val userCommandExecutions = ExecutionMap<GuildUserCommandEvent>()

fun GuildChatInputCommand.onExecute(block: CommandExecution<GuildChatInputEvent>): GuildChatInputCommand =
    also { chatInputExecutions[it.id] = DebuggableExecution(block, GuildChatInputEvent::class) }

fun GuildMessageCommand.onExecute(block: CommandExecution<GuildMessageCommandEvent>): GuildMessageCommand =
    also { messageCommandExecutions[it.id] = DebuggableExecution(block, GuildMessageCommandEvent::class) }

fun GuildUserCommand.onExecute(block: CommandExecution<GuildUserCommandEvent>): GuildUserCommand =
    also { userCommandExecutions[it.id] = DebuggableExecution(block, GuildUserCommandEvent::class) }


@Suppress("UNCHECKED_CAST")
suspend fun registerCommandListener() = kord.events.buffer()
    .filterIsInstance<GuildAppCommandEvent>()
    .onEach {
        val map = when (it) {
            is GuildChatInputEvent -> chatInputExecutions
            is GuildMessageCommandEvent -> messageCommandExecutions
            is GuildUserCommandEvent -> userCommandExecutions
        } as? ExecutionMap<GuildAppCommandEvent> ?: error("Unspecified GuildAppCommandEvent instance ${it::class}.")
        val debuggableExecution = map[it.interaction.invokedCommandId] ?: return@onEach
        try {
            debuggableExecution.invoke(it)
        } catch (exception: Exception) {
            commandLogger.warn(exception) { debuggableExecution }
            if (exception is IllegalStateException) it.interaction.respondEphemeral {
                content = "${Emojis.x} An internal server error occurred."
            } else it.interaction.respondEphemeral {
                content = "${Emojis.x} ${exception.message}"
            }
        }
    }

// Source = mu.internal.KLoggerNameResolver.name
private fun name(block: CommandExecution<*>): String {
    val name = block.javaClass.name
    return when {
        "Kt$" in name -> name.substringBefore("Kt$")
        "$" in name -> name.substringBefore("$")
        else -> name
    }
}

class DebuggableExecution<T : GuildAppCommandEvent> internal constructor(
    private val block: CommandExecution<T>,
    inputClass: KClass<T>
) : CommandExecution<T> by block {
    private val inputClassName = inputClass.simpleName
    private val blockLocation = name(block)
    override fun toString(): String =
        "Command execution of type <$inputClassName> defined in <$blockLocation> resulted in an error."
}