package dev.kason.eightlakes.discord.main

import dev.kason.eightlakes.core.englishOrdinal
import dev.kason.eightlakes.discord.*
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.create.embed

suspend fun _profileCommand() = chatInputCommand(
    "profile",
    "Displays basic information about you."
) {
    user("user", "The user who you want to profile. Defaults to yourself.", notRequired)
}.onExecute {
    val student = student()
    val user = interaction.user
    interaction.respondPublic {
        embed {
            title = "${user.nameWithDiscriminator}'s profile"
            description = student.biography ?: ""
            image = user.avatar?.url ?: user.defaultAvatar.url
            footer {
                text = "${user.order().englishOrdinal()} member"
            }
        }
    }
}