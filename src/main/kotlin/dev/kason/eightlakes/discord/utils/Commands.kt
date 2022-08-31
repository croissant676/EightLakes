package dev.kason.eightlakes.discord.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.kason.eightlakes.core.models.Student
import dev.kason.eightlakes.core.models.Students
import dev.kason.eightlakes.core.utils.InternalOutput
import dev.kason.eightlakes.core.utils.Possible
import dev.kason.eightlakes.core.utils.leftValue
import dev.kason.eightlakes.core.utils.rightValue
import dev.kason.eightlakes.guild
import dev.kason.eightlakes.guildId
import dev.kason.eightlakes.kord
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.application.GuildApplicationCommand
import dev.kord.core.entity.application.GuildChatInputCommand
import dev.kord.core.entity.application.GuildMessageCommand
import dev.kord.core.entity.application.GuildUserCommand
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.MessageCommandCreateBuilder
import dev.kord.rest.builder.interaction.UserCommandCreateBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.toList
import mu.KLogger
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// event types
typealias GuildChatInputEvent = GuildChatInputCommandInteractionCreateEvent
typealias GuildUserCommandEvent = GuildUserCommandInteractionCreateEvent
typealias GuildMessageCommandEvent = GuildMessageCommandInteractionCreateEvent
typealias GuildAppCommandEvent = GuildApplicationCommandInteractionCreateEvent
private typealias CommandExecution<T> = suspend T.() -> Unit

suspend fun chatInputCommand(
    name: String,
    description: String,
    builder: ChatInputCreateBuilder.() -> Unit = {}
): GuildChatInputCommand {
    val guildCommands = guild.getApplicationCommands().toList()
    val filtered =
        guildCommands.filter {
            it is GuildChatInputCommand && it.name == name && it.data.description == description
        }
    if (filtered.isNotEmpty()) return filtered.first() as GuildChatInputCommand
    return kord.createGuildChatInputCommand(
        guildId, name, description, builder
    )
}

suspend fun messageCommand(
    name: String,
    builder: MessageCommandCreateBuilder.() -> Unit = {}
): GuildMessageCommand {
    val guildCommands = guild.getApplicationCommands().toList()
    val filtered =
        guildCommands.filter {
            it is GuildMessageCommand && it.name == name
        }
    if (filtered.isNotEmpty()) return filtered.first() as GuildMessageCommand
    return kord.createGuildMessageCommand(
        guildId, name, builder
    )
}

suspend fun userCommand(
    name: String,
    builder: UserCommandCreateBuilder.() -> Unit = {}
): GuildUserCommand {
    val guildCommands = guild.getApplicationCommands().toList()
    val filtered =
        guildCommands.filter {
            it is GuildUserCommand && it.name == name
        }
    if (filtered.isNotEmpty()) return filtered.first() as GuildUserCommand
    return kord.createGuildUserCommand(
        guildId, name, builder
    )
}


private val typeMap = mapOf(
    GuildChatInputCommand::class to GuildChatInputEvent::class,
    GuildMessageCommand::class to GuildMessageCommandEvent::class,
    GuildUserCommand::class to GuildUserCommandEvent::class
)

private val chatInputExecutions =
    mutableMapOf<Snowflake, CommandExecution<GuildChatInputEvent>>()
private val messageCommandExecutions =
    mutableMapOf<Snowflake, CommandExecution<GuildMessageCommandEvent>>()
private val userCommandExecutions =
    mutableMapOf<Snowflake, CommandExecution<GuildUserCommandEvent>>()

@Suppress("UNCHECKED_CAST")
@InternalOutput
private inline fun <reified E : GuildAppCommandEvent, reified T : GuildApplicationCommand> T.onExecute(
    crossinline _block: CommandExecution<E>
): Possible<T> {
    val tClass = T::class
    val eClass = E::class
    val expectedType = typeMap[tClass]
        ?: return "Your types have to be one of ${typeMap.keys}. Inputted type = ${T::class}".left()
    if (expectedType != E::class)
        return "The event type for the given command doesn't match the expected event type: $tClass to $eClass, expected $expectedType".left()
    val executionMap = when (this) {
        is GuildChatInputCommand -> chatInputExecutions
        is GuildMessageCommand -> messageCommandExecutions
        is GuildUserCommand -> userCommandExecutions
        else -> throw IllegalStateException("This should never happen.")
    } as MutableMap<Snowflake, CommandExecution<E>>
    val block: CommandExecution<E> = {
        if (this is GuildChatInputEvent)
            beforeChatInput()
        _block()
    }
    executionMap[id] = block
    return right()
}

suspend fun addMessageListener() {
    kord.on<GuildChatInputEvent> { chatInputExecutions[interaction.invokedCommandId]?.invoke(this) }
    kord.on<GuildMessageCommandEvent> { messageCommandExecutions[interaction.invokedCommandId]?.invoke(this) }
    kord.on<GuildUserCommandEvent> { userCommandExecutions[interaction.invokedCommandId]?.invoke(this) }
}

private val commandLogger: KLogger = KotlinLogging.logger { }

private suspend fun GuildChatInputEvent.beforeChatInput() {
    commandLogger.debug { "Called command `/${interaction.command.rootName}` with values: ${interaction.command.options}" }
}

fun GuildChatInputCommand.onExecute(block: CommandExecution<GuildChatInputEvent>): Possible<GuildChatInputCommand> =
    onExecute<GuildChatInputEvent, GuildChatInputCommand>(block)

fun GuildMessageCommand.onExecute(block: CommandExecution<GuildMessageCommandEvent>): Possible<GuildMessageCommand> =
    onExecute<GuildMessageCommandEvent, GuildMessageCommand>(block)

fun GuildUserCommand.onExecute(block: CommandExecution<GuildUserCommandEvent>): Possible<GuildUserCommand> =
    onExecute<GuildUserCommandEvent, GuildUserCommand>(block)

val GuildAppCommandEvent.user: User get() = interaction.user
suspend fun GuildAppCommandEvent.member(): Member = guild.getMember(user.id)

suspend fun InteractionCreateEvent.student(): Either<StudentSignInError, Student> {
    val discordId = interaction.user.id
    val possibleStudents =
        newSuspendedTransaction { Student.find { Students.discordId eq discordId }.toList() }
    if (possibleStudents.isEmpty()) return StudentSignInError.NotRegistered.left()
    val student = possibleStudents.first()
    if (!student.verified) return StudentSignInError.NotVerified.left()
    return student.right()
}

enum class StudentSignInError {
    NotRegistered,
    NotVerified;

    val message: String
        get() = when (this) {
            NotRegistered -> "You aren't registered yet. Use the command `/signup` in order to register!"
            NotVerified -> "You need to verify your email first. Check your school email (the one that ends with " +
                    "`students.katyisd.org`) and do `/verify {token}`"
        }
}

private val defaultStudentSignInErrorAction: suspend GuildAppCommandEvent.(StudentSignInError) -> Unit = {
    interaction.respondPublic {
        content = "${Emojis.x} ${it.message}"
    }
}

val safeResponse: suspend GuildAppCommandEvent.(StudentSignInError) -> Unit = {}

suspend fun GuildAppCommandEvent.studentOrElse(
    onError: suspend GuildAppCommandEvent.(StudentSignInError) -> Unit = defaultStudentSignInErrorAction
): Student? {
    val student = student()
    if (student.isLeft()) {
        onError(student.leftValue()!!)
        return null
    }
    return (student.rightValue())
}