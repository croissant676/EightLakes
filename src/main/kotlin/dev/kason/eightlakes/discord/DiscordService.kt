package dev.kason.eightlakes.discord

import com.typesafe.config.Config
import dev.kason.eightlakes.utils.*
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

private typealias ExecutionMap<E> = HashMap<Snowflake, CommandExecution<E>>
private typealias CommandExecution<T> = suspend T.() -> Unit

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
        execution: CommandExecution<E>
    ) {
        logger.debug { "Registered command ${command.name} of type ${command.type.value}." }
        when (command.type) {
            ApplicationCommandType.ChatInput -> chatInputExecutions[command.id] =
                execution as CommandExecution<GuildChatInputEvent>
            ApplicationCommandType.User -> userExecutions[command.id] =
                execution as CommandExecution<GuildUserCommandEvent>
            ApplicationCommandType.Message -> messageExecutions[command.id] =
                execution as CommandExecution<GuildMessageCommandEvent>
            else -> error("Unknown application command type.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun init() {
        if (allCommands.isEmpty()) loadCommandsFromKord()
        kord.on<GuildAppCommandEvent> {
            if (interaction.applicationId != application.id) return@on
            val executionMap = when (interaction.invokedCommandType) {
                ApplicationCommandType.ChatInput -> chatInputExecutions
                ApplicationCommandType.Message -> messageExecutions
                ApplicationCommandType.User -> userExecutions
                else -> error("Unknown application command type.")
            } as ExecutionMap<GuildAppCommandEvent>
            val execution: CommandExecution<GuildAppCommandEvent> = executionMap[interaction.invokedCommandId]
                ?: return@on logger.warn { "Unknown command ${interaction.invokedCommandName} of type of type ${interaction.invokedCommandType}." }
            val loggingPrefix =
                "Interaction (${interaction.id}:${interaction.invokedCommandType}:${interaction.invokedCommandName}):"
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

    private val controllers = mutableSetOf<DiscordController>()
    val discordControllers: List<DiscordController> get() = controllers.toList()

    suspend fun register(discordController: DiscordController) {
        controllers += discordController
        discordController.loadCommands()
    }

    // Subcommand nesting

    internal val subCommandBuilderRegistry = mutableMapOf<String, DiscordController.ExecutionNesting>()

}