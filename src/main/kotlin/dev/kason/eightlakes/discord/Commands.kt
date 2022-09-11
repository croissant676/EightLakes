package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kason.eightlakes.discord.main.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.application.*
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import dev.kord.x.emoji.Emojis
import mu.KotlinLogging
import uy.klutter.config.typesafe.value

typealias GuildChatInputEvent = GuildChatInputCommandInteractionCreateEvent
typealias GuildUserCommandEvent = GuildUserCommandInteractionCreateEvent
typealias GuildMessageCommandEvent = GuildMessageCommandInteractionCreateEvent
typealias GuildAppCommandEvent = GuildApplicationCommandInteractionCreateEvent
typealias ExecutionMap<E> = HashMap<Snowflake, SuspendingExecution<E>>
typealias SuspendingExecution<T> = suspend T.() -> Unit

private val commandLogger = KotlinLogging.logger {}

// If deletions are turned off
suspend fun chatInputCommand(
    name: String,
    description: String,
    block: ChatInputCreateBuilder.() -> Unit = {}
) = CommandCache.retrieveOfType<GuildChatInputCommand>().firstOrNull {
    it.name == name
} ?: CommandCache.insert(kord.createGuildChatInputCommand(guildId, name, description, block))

suspend fun messageCommand(
    name: String,
    block: MessageCommandCreateBuilder.() -> Unit = {}
) = CommandCache.retrieveOfType<GuildMessageCommand>().firstOrNull {
    it.name == name
} ?: CommandCache.insert(kord.createGuildMessageCommand(guildId, name, block))

suspend fun userCommand(
    name: String,
    block: UserCommandCreateBuilder.() -> Unit = {}
): GuildUserCommand = CommandCache.retrieveOfType<GuildUserCommand>().firstOrNull {
    it.name == name
} ?: CommandCache.insert(kord.createGuildUserCommand(guildId, name, block))

private val chatInputExecutions = ExecutionMap<GuildChatInputEvent>()
private val messageCommandExecutions = ExecutionMap<GuildMessageCommandEvent>()
private val userCommandExecutions = ExecutionMap<GuildUserCommandEvent>()

fun GuildChatInputCommand.onExecute(block: SuspendingExecution<GuildChatInputEvent>): GuildChatInputCommand =
    log { chatInputExecutions[id] = block }

fun GuildMessageCommand.onExecute(block: SuspendingExecution<GuildMessageCommandEvent>): GuildMessageCommand =
    log { messageCommandExecutions[id] = block }

fun GuildUserCommand.onExecute(block: SuspendingExecution<GuildUserCommandEvent>): GuildUserCommand =
    log { userCommandExecutions[id] = block }

private fun <T : GuildApplicationCommand> T.log(block: T.() -> Unit): T {
    commandLogger.debug { "Registering command `/$name` of type <${this::class}>." }
    return apply(block)
}

@Suppress("UNCHECKED_CAST")
suspend fun registerCommandListener() {
    CommandCache.retrieveCommands()
    // Register the actual command listener now
    kord.on<GuildAppCommandEvent> {
        if (interaction.applicationId != appId) return@on // Ignore commands that aren't ours.
        val map = when (this) {
            is GuildChatInputEvent -> chatInputExecutions
            is GuildMessageCommandEvent -> messageCommandExecutions
            is GuildUserCommandEvent -> userCommandExecutions
        } as? ExecutionMap<GuildAppCommandEvent> ?: error("Unspecified GuildAppCommandEvent instance ${this::class}.")
        val execution = map[interaction.invokedCommandId]
            ?: return@on commandLogger.debug { "Unknown command `/${interaction.invokedCommandName}` of type <${this::class}> was invoked." }
        val loggerPrefix = "[Interaction ${interaction.id}]"
        commandLogger.debug { "$loggerPrefix Executing function `/${interaction.invokedCommandName}`, type <${this::class}>" }
        try {
            execution(this)
            commandLogger.debug { "$loggerPrefix Finished executing function `/${interaction.invokedCommandName}`" }
        } catch (exception: Exception) {
            commandLogger.warn(exception) { "$loggerPrefix An execution occurred while invoking command `/${interaction.invokedCommandName}.`" }
            interaction.respondEphemeral {
                this.content = if (exception is IllegalStateException) "${Emojis.x} An internal server error occurred."
                else "${Emojis.x} ${exception.message}"
            }
        }
    }
}

val required: OptionsBuilder.() -> Unit = { required = true }
val notRequired: OptionsBuilder.() -> Unit = { required = false }

suspend fun registerAllCommands() {
    if (config.value("bot.delete-commands").asBoolean(false)) {
        val commands = CommandCache.all()
        logger.debug { "Deleting commands ${commands.map { it.name }}" }
        commands.forEach {
            it.delete()
        }
        CommandCache.clear()
    }
    _signupCommand()
    _verificationCommand()
    _profileCommand()
    _adminCourseCommands()
}