package dev.kason.eightlakes

import dev.kason.eightlakes.utils.ConfigAware
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.application.*
import dev.kord.core.entity.interaction.*
import dev.kord.rest.builder.interaction.*
import org.kodein.di.*

private typealias ChatInputExecution = suspend GuildChatInputEvent.() -> Unit

@Suppress("MemberVisibilityCanBePrivate")
abstract class DiscordController(override val di: DI) : ConfigAware(di) {

    companion object {
        val Required: OptionsBuilder.() -> Unit = { required = true }
        val NotRequired: OptionsBuilder.() -> Unit = { required = false }
    }

    protected val kord: Kord by lazy { di.direct.instance() }
    protected val guild: Guild by lazy { di.direct.instance() }
    protected val discordService: DiscordService by lazy { di.direct.instance() }

    abstract suspend fun loadCommands()

    suspend fun chatInputCommand(
        name: String,
        description: String,
        block: ChatInputCreateBuilder.() -> Unit = {}
    ): GuildChatInputCommand {
        val command = discordService.getCommands()
            .filterIsInstance<GuildChatInputCommand>()
            .firstOrNull {
                it.name == name
            }
        if (command != null) return command
        val chatInputCommand =
            kord.createGuildChatInputCommand(guild.id, name, description, block)
        discordService.addCommand(chatInputCommand)
        return chatInputCommand
    }

    suspend fun messageCommand(
        name: String,
        block: MessageCommandCreateBuilder.() -> Unit = {}
    ): GuildMessageCommand {
        val command = discordService.getCommands()
            .filterIsInstance<GuildMessageCommand>()
            .firstOrNull {
                it.name == name
            }
        if (command != null) return command
        val messageCommand =
            kord.createGuildMessageCommand(guild.id, name, block)
        discordService.addCommand(messageCommand)
        return messageCommand
    }

    suspend fun userCommand(
        name: String,
        block: UserCommandCreateBuilder.() -> Unit = {}
    ): GuildUserCommand {
        val command = discordService.getCommands()
            .filterIsInstance<GuildUserCommand>()
            .firstOrNull {
                it.name == name
            }
        if (command != null) return command
        val userCommand =
            kord.createGuildUserCommand(guild.id, name, block)
        discordService.addCommand(userCommand)
        return userCommand
    }

    fun GuildChatInputCommand.onExecute(block: suspend GuildChatInputEvent.() -> Unit) {
        discordService.registerCommand(this, block)
    }

    fun GuildMessageCommand.onExecute(block: suspend GuildMessageCommandEvent.() -> Unit) {
        discordService.registerCommand(this, block)
    }

    fun GuildUserCommand.onExecute(block: suspend GuildUserCommandEvent.() -> Unit) {
        discordService.registerCommand(this, block)
    }

    suspend fun parentCommand(
        name: String,
        description: String,
        builder: ChatInputCreateBuilder.(ExecutionNesting) -> Unit
    ): GuildChatInputCommand {
        val executionNesting = ExecutionNesting()
        val newBuilder: ChatInputCreateBuilder.() -> Unit = {
            discordService.subCommandBuilderRegistry[name] = executionNesting
            builder(executionNesting)
            discordService.subCommandBuilderRegistry.remove(name)
        }
        val chatInputCommand =
            chatInputCommand(name, description, newBuilder)
        chatInputCommand.onExecute(executionNesting.createChatInputExecution())
        return chatInputCommand
    }

    // Difference from library code being that it returns the builder.
    fun ChatInputCreateBuilder.subCommand(
        name: String,
        description: String,
        builder: SubCommandBuilder.() -> Unit = {}
    ): SubCommandBuilder {
        if (options == null) options = mutableListOf()
        val element = SubCommandBuilder(name, description).apply(builder)
        options!!.add(element)
        return element
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    fun GroupCommandBuilder.command(
        name: String,
        description: String,
        builder: SubCommandBuilder.() -> Unit = {}
    ): SubCommandBuilder {
        if (options == null) options = mutableListOf()
        val element = SubCommandBuilder(name, description).apply(builder)
        options!!.add(element)
        return element
    }

    context (ChatInputCreateBuilder)
    fun SubCommandBuilder.onExecute(block: ChatInputExecution) {
        val currentExecutionNesting = discordService.subCommandBuilderRegistry[this@ChatInputCreateBuilder.name]!!
        currentExecutionNesting.subCommand(this.name, block)
    }

    fun ChatInputCreateBuilder.group(
        name: String,
        description: String = "",
        builder: GroupCommandBuilder.() -> Unit = {}
    ): GroupCommandBuilder {
        if (options == null) options = mutableListOf()
        val currentExecutionNesting = discordService.subCommandBuilderRegistry[this.name]!!
        currentExecutionNesting.currentGroup = name
        val element = GroupCommandBuilder(name, description).apply(builder)
        currentExecutionNesting.currentGroup = null
        options!!.add(element)
        return element
    }

    class ExecutionNesting {
        var currentGroup: String? = null
        private val mapping = mutableMapOf<String, ChatInputExecution>()

        fun subCommand(name: String, chatInputExecution: ChatInputExecution) {
            if (currentGroup != null) {
                mapping["$currentGroup.$name"] = chatInputExecution
            } else {
                mapping[name] = chatInputExecution
            }
        }

        fun createChatInputExecution(): ChatInputExecution = {
            val command = interaction.command
            if (command is SubCommand) {
                val execution = mapping[command.name]
                if (execution != null) {
                    execution()
                } else {
                    error("No execution found for sub command ${command.name}")
                }
            } else if (command is GroupCommand) {
                val searchName = "${command.groupName}.${command.name}"
                val execution = mapping[searchName]
                if (execution != null) {
                    execution()
                } else {
                    error("No execution found for sub command ${command.groupName}.${command.name}")
                }
            } else {
                error("Unknown command type: ${interaction.command}, ${interaction.command::class}")
            }
        }
    }

    // Utils

}