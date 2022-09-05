package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kord.common.entity.*
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.application.*
import dev.kord.core.event.interaction.*
import dev.kord.core.on
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

// If deletions are turned off
suspend fun chatInputCommand(
    name: String,
    description: String,
    block: ChatInputCreateBuilder.() -> Unit = {}
): GuildChatInputCommand = guild.getApplicationCommands().filter {
    it.applicationId == appId && it.type == ApplicationCommandType.ChatInput && it.name == name
}.firstOrNull() as? GuildChatInputCommand ?: kord.createGuildChatInputCommand(guildId, name, description, block)

suspend fun messageCommand(
    name: String,
    block: MessageCommandCreateBuilder.() -> Unit = {}
): GuildMessageCommand = guild.getApplicationCommands().filter {
    it.applicationId == appId && it.type == ApplicationCommandType.Message && it.name == name
}.firstOrNull() as? GuildMessageCommand ?: kord.createGuildMessageCommand(guildId, name, block)

suspend fun userCommand(
    name: String,
    block: UserCommandCreateBuilder.() -> Unit = {}
): GuildUserCommand = guild.getApplicationCommands().filter {
    it.applicationId == appId && it.type == ApplicationCommandType.User && it.name == name
}.firstOrNull() as? GuildUserCommand ?: kord.createGuildUserCommand(guildId, name, block)

private val chatInputExecutions = ExecutionMap<GuildChatInputEvent>()
private val messageCommandExecutions = ExecutionMap<GuildMessageCommandEvent>()
private val userCommandExecutions = ExecutionMap<GuildUserCommandEvent>()

fun GuildChatInputCommand.onExecute(block: CommandExecution<GuildChatInputEvent>): GuildChatInputCommand =
    also { chatInputExecutions[id] = debuggable(block) }

fun GuildMessageCommand.onExecute(block: CommandExecution<GuildMessageCommandEvent>): GuildMessageCommand =
    also { messageCommandExecutions[id] = debuggable(block) }

fun GuildUserCommand.onExecute(block: CommandExecution<GuildUserCommandEvent>): GuildUserCommand =
    also { userCommandExecutions[id] = debuggable(block) }


@Suppress("UNCHECKED_CAST")
suspend fun registerCommandListener() = kord.on<GuildAppCommandEvent> {
    val map = when (this) {
        is GuildChatInputEvent -> chatInputExecutions
        is GuildMessageCommandEvent -> messageCommandExecutions
        is GuildUserCommandEvent -> userCommandExecutions
    } as? ExecutionMap<GuildAppCommandEvent> ?: error("Unspecified GuildAppCommandEvent instance ${this::class}.")
    val debuggableExecution = map[interaction.invokedCommandId]
        ?: return@on commandLogger.debug { "Unknown command \"${interaction.invokedCommandName}\" of type <${this::class}> was invoked." }
    try {
        debuggableExecution.invoke(this)
    } catch (exception: Exception) {
        commandLogger.warn(exception) { debuggableExecution }
        val content = if (exception is IllegalStateException) "${Emojis.x} An internal server error occurred."
        else "${Emojis.x} ${exception.message}"
        interaction.respondEphemeral {
            this.content = content
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

private inline fun <reified T : GuildAppCommandEvent> GuildApplicationCommand.debuggable(
    noinline block: CommandExecution<T>
): DebuggableExecution<T> {
    val execution = DebuggableExecution(block, T::class)
    commandLogger.debug { "Registering command \"$name\" of type <${T::class}> from <${execution.blockLocation}>" }
    return execution
}

class DebuggableExecution<T : GuildAppCommandEvent> internal constructor(
    private val block: CommandExecution<T>,
    inputClass: KClass<T>
) : CommandExecution<T> by block {
    private val inputClassName = inputClass.simpleName
    internal val blockLocation = name(block)
    override fun toString(): String =
        "Command execution of type <$inputClassName> defined in <$blockLocation> resulted in an error."
}

val required: BaseChoiceBuilder<*>.() -> Unit = { required = true }
val notRequired: BaseChoiceBuilder<*>.() -> Unit = { required = false }

suspend fun registerAllCommands() {
    val commands = guild.getApplicationCommands().filter { it.applicationId == appId }.toList()
    logger.debug { "Deleting commands ${commands.map { it.name }}" }
    commands.forEach {
        it.delete()
    }
    _signupCommand()
}