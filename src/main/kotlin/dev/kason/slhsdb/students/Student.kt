package dev.kason.slhsdb.students

import dev.kason.slhsdb.kord
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on

@kotlinx.serialization.Serializable
data class Student(
    val discordId: Snowflake,


    )

suspend fun userModule() {
    val command = kord.createGlobalChatInputCommand(
        "ping",
        "A simple command that returns pong along with your name and discriminator"
    )
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()

        response.respond {
            this.content = "Hello World!"
        }
    }
}