package dev.kason.slhsdb.disc

import arrow.core.Either
import dev.kason.slhsdb.CommandExecution
import dev.kason.slhsdb.core.*
import dev.kason.slhsdb.idFromString
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.litote.kmongo.eq

suspend fun GuildChatInputCommandInteractionCreateEvent.ensureVerified(
    block: CommandExecution = {
        interaction.deferEphemeralResponse().respond {
            content = "You aren't registered in the system yet. Use the command `/signup` to join!"
        }
    }
): Either<Unit, Student> {
    val userId = interaction.user.id
    val student = studentDatabase.findOne(Student::discordId eq userId)
    return if (student != null) Either.Right(student)
    else Either.Left(block())
}

suspend fun GuildChatInputCommandInteractionCreateEvent.ensureAdministrator(
    block: CommandExecution = {
        interaction.deferEphemeralResponse().respond {
            content = "You need to have administrator permissions to use this command."
        }
    }
): Either<Unit, Student> {
    val student = studentDatabase.findOne(Student::discordId eq interaction.user.id)
    return if (interaction.user.getPermissions().values.contains(Permission.Administrator)) Either.Right(student!!)
    else Either.Left(block())
}

suspend fun GuildChatInputCommandInteractionCreateEvent.receiveCourse(): Course? = kotlin.runCatching {
    val strings = interaction.command.strings
    val name = strings["name"]
    if (name != null) {
        return courseDatabase.findOne(Course::simpleName eq name)
    }
    val id = strings["id"]
    if (id != null) {
        return courseDatabase.findOneById(idFromString(id))
    }
    val period = strings["period"]
    if (period != null) {
        ensureVerified().tap {
            val studentPeriod = parsePeriod(period)
            return it.courses[studentPeriod]?.course()
        }
    }
    return null
}.getOrNull()

fun Int.toOrdinal(): String = when (this % 10) {
    1 -> "${this}st"
    2 -> "${this}nd"
    3 -> "${this}rd"
    else -> "${this}th"
}

