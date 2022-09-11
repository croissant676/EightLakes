package dev.kason.eightlakes.discord

import dev.kason.eightlakes.core.*
import dev.kason.eightlakes.core.data.*
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
    } ?: illegalArg("You aren't registered yet. Use the command `/signup` in order to register.")
    return if (student.isVerified) student else illegalArg(
        """
        You need to verify your email first. Check your school email (the one that ends with `students.katyisd.org`)
        and do `/verify [token]` in order to verify your account.
        """.trimIndent()
    )
}

