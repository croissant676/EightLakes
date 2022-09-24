package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kord.common.entity.*
import dev.kord.core.*
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.*
import dev.kord.core.entity.application.GuildApplicationCommand
import dev.kord.core.event.interaction.*
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.toSet
import mu.KLogging
import org.kodein.di.*
import kotlin.system.measureTimeMillis

typealias GuildChatInputEvent = GuildChatInputCommandInteractionCreateEvent
typealias GuildUserCommandEvent = GuildUserCommandInteractionCreateEvent
typealias GuildMessageCommandEvent = GuildMessageCommandInteractionCreateEvent
typealias GuildAppCommandEvent = GuildApplicationCommandInteractionCreateEvent

private typealias ExecutionMap<E> = HashMap<Snowflake, Execution<E>>
private typealias Execution<T> = suspend T.() -> Unit

typealias GuildButtonEvent = GuildButtonInteractionCreateEvent
typealias GuildMenuEvent = GuildSelectMenuInteractionCreateEvent
typealias GuildComponentEvent = GuildComponentInteractionCreateEvent

@Suppress("DuplicatedCode")
class DiscordService(override val di: DI) : ConfigAware(di) {
    companion object : KLogging(), ModuleProducer {
        override suspend fun createModule(config: Config): DI.Module {
            return DI.Module(name = "discord_module") {
                bindSingleton { DiscordService(di) }

            }
        }
    }

    private val kord: Kord by di.instance()
    private val application: Application by di.instance()

    private val chatInputExecutions = ExecutionMap<GuildChatInputEvent>()
    private val userExecutions = ExecutionMap<GuildUserCommandEvent>()
    private val messageExecutions = ExecutionMap<GuildMessageCommandEvent>()

    private val allCommands: MutableSet<GuildApplicationCommand> = mutableSetOf()

    fun getCommands(): Set<GuildApplicationCommand> = allCommands

    @Suppress("UNCHECKED_CAST")
    fun <E : GuildAppCommandEvent> registerCommand(
        command: GuildApplicationCommand,
        execution: Execution<E>
    ) {
        logger.debug { "Registered command ${command.name} of type ${command.type.value}." }
        when (command.type) {
            ApplicationCommandType.ChatInput -> chatInputExecutions[command.id] =
                execution as Execution<GuildChatInputEvent>
            ApplicationCommandType.User -> userExecutions[command.id] =
                execution as Execution<GuildUserCommandEvent>
            ApplicationCommandType.Message -> messageExecutions[command.id] =
                execution as Execution<GuildMessageCommandEvent>
            else -> error("Unknown application command type.")
        }
    }

    suspend fun init() {
        if (allCommands.isEmpty()) loadCommandsFromKord()
        initControllers()
        commandSystem()

    }

    @Suppress("UNCHECKED_CAST")
    suspend fun commandSystem() {
        kord.on<GuildAppCommandEvent> {
            if (interaction.applicationId != application.id) return@on
            val executionMap = when (interaction.invokedCommandType) {
                ApplicationCommandType.ChatInput -> chatInputExecutions
                ApplicationCommandType.Message -> messageExecutions
                ApplicationCommandType.User -> userExecutions
                else -> error("Unknown application command type.")
            } as ExecutionMap<GuildAppCommandEvent>
            val execution: Execution<GuildAppCommandEvent> = executionMap[interaction.invokedCommandId]
                ?: return@on logger.warn { "Unknown command ${interaction.invokedCommandName} of type of type ${interaction.invokedCommandType}." }
            val loggingPrefix =
                "Command (${interaction.id}:${interaction.invokedCommandType}:${interaction.invokedCommandName}):"
            logger.debug { "$loggingPrefix Starting interaction execution." }
            try {
                val time = measureTimeMillis {
                    execution(this)
                }
                logger.debug { "$loggingPrefix Finished interaction execution in $time ms." }
            } catch (exception: Exception) {
                logger.warn(exception) { "$loggingPrefix An exception occurred while intercepting execution." }
                interaction.respondEphemeral {
                    content = if (exception is IllegalStateException) "${Emojis.x} An internal server error occurred."
                    else "${Emojis.x} ${exception.message}"
                }
            }
        }
    }

    private suspend fun loadCommandsFromKord() {
        val guild = di.direct.instanceOrNull() ?: createGuildBean()
        val guildApplicationCommands = guild.getApplicationCommands()
            .toSet()
            .filter {
                it.applicationId != application.id
            }
        allCommands += guildApplicationCommands
    }

    private suspend fun createGuildBean(): Guild {
        val guild = kord.getGuild(
            Snowflake(config.getString("bot.guild"))
        ) ?: error("Property bot.guild does not represent actual guild.")
        di.newInstance { guild }
        return guild
    }

    fun addCommand(chatInputCommand: GuildApplicationCommand) {
        allCommands += chatInputCommand
    }

    val controllers = mutableSetOf<DiscordController>()

    private suspend fun initControllers() {
        controllers.forEach {
            kotlin.runCatching {
                it.loadCommands()
            }.onFailure { exception ->
                logger.warn(exception) { "An exception occurred while loading commands for controller ${it::class.simpleName}." }
            }
        }
    }

    internal val subCommandBuilderRegistry = mutableMapOf<String, DiscordController.ExecutionNesting>()

    private val buttonExecutions = mutableMapOf<String, Execution<GuildButtonEvent>>()
    private val selectMenuExecutions = mutableMapOf<String, Execution<GuildMenuEvent>>()

    suspend fun componentInteractionSystem() {
        kord.on<GuildButtonEvent> {
            val execution = buttonExecutions[interaction.componentId]
                ?: return@on logger.warn { "Unknown button interaction ${interaction.id}." }
            val loggingPrefix = "Button (${interaction.id}):"
            logger.debug { "$loggingPrefix Starting interaction execution." }
            try {
                val time = measureTimeMillis {
                    execution(this)
                }
                logger.debug { "$loggingPrefix Finished interaction execution in $time ms." }
            } catch (exception: Exception) {
                logger.warn(exception) { "$loggingPrefix An exception occurred while intercepting execution." }
                interaction.respondEphemeral {
                    content = if (exception is IllegalStateException) "${Emojis.x} An internal server error occurred."
                    else "${Emojis.x} ${exception.message}"
                }
            }
        }
        kord.on<GuildMenuEvent> {
            val execution = selectMenuExecutions[interaction.componentId]
                ?: return@on logger.warn { "Unknown select menu interaction ${interaction.id}." }
            val loggingPrefix = "Menu (${interaction.id}):"
            logger.debug { "$loggingPrefix Starting interaction execution." }
            try {
                val time = measureTimeMillis {
                    execution(this)
                }
                logger.debug { "$loggingPrefix Finished interaction execution in $time ms." }
            } catch (exception: Exception) {
                logger.warn(exception) { "$loggingPrefix An exception occurred while intercepting execution." }
                interaction.respondEphemeral {
                    content = if (exception is IllegalStateException) "${Emojis.x} An internal server error occurred."
                    else "${Emojis.x} ${exception.message}"
                }
            }
        }
    }

    fun registerButtonInteraction(
        id: String,
        execution: Execution<GuildButtonEvent>
    ) {
        buttonExecutions[id] = execution
    }

    fun registerSelectMenuInteraction(
        customId: String,
        block: Execution<GuildMenuEvent>
    ) {
        selectMenuExecutions[customId] = block
    }

}