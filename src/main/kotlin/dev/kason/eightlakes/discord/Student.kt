package dev.kason.eightlakes.discord

import dev.kason.eightlakes.core.*
import dev.kord.core.event.interaction.InteractionCreateEvent
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

suspend fun InteractionCreateEvent.studentOrNull(): Student? {
    val discordId = interaction.user.id
    val student = suspendTransaction {
        Student.find(Students.discordId eq discordId).singleOrNull()
    } ?: return null
    return if (student.isVerified) student else null
}

suspend fun InteractionCreateEvent.student(): Student {
    val discordId = interaction.user.id
    val student = suspendTransaction {
        Student.find(Students.discordId eq discordId).singleOrNull()
    } ?: illegalArg(response("not-registered"))
    return if (student.isVerified) student else illegalArg(response("not-verified"))
}