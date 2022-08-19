package dev.kason.slhsdb.disc

import arrow.core.Either
import dev.kason.slhsdb.CommandExecution
import dev.kason.slhsdb.core.Student
import dev.kason.slhsdb.core.studentDatabase
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