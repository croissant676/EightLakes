package dev.kason.eightlakes.discord.main

import dev.kason.eightlakes.core.*
import dev.kason.eightlakes.discord.*
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.interaction.string
import dev.kord.x.emoji.Emojis
import kotlinx.datetime.LocalDate

suspend fun _signupCommand() = chatInputCommand(
    "signup",
    "Registers yourself as a student."
) {
    string("id", "Your student id.", required)
    string("first", "Your first name, as it would appear on your school id.", required)
    string("last", "Your last name, as it would appear on your school id.", required)
    string("birthday", "Your birthday, in the format `MM/dd/yyyy`.", required)
    string("middle", "Your middle name (if you have one), as it would appear on your school id.", notRequired)
    string("preferred", "Your preferred name (if you have one)", notRequired)
}.onExecute {
    val args = interaction.command.strings
    val id by args
    val first by args
    val last by args
    val birthday by args
    val middle = args["middle"]
    val preferred = args["preferred"]
    val discordId = interaction.user.id
    val (month, day, year) = birthday.split("/").map { it.toInt() }
    val date = LocalDate(year, month, day)
    registerStudent(
        first,
        middle,
        last,
        preferred,
        id,
        date,
        discordId
    )
    interaction.respondEphemeral {
        content = "${Emojis.whiteCheckMark} Thanks for registering! Now all you need to do is go to your school " +
                "email account. (the one that ends with `@students.katyisd.org`) and get the token. Then, use the command" +
                "`/verify [token]` to finish the signup process."
    }
}

suspend fun _verificationCommand() = chatInputCommand(
    "verify",
    "Verifies your discord account."
) {
    string("token", "The token you received.", required)
}.onExecute {
    val token by interaction.command.strings
    val discordId = interaction.user.id
    finishVerification(
        token, discordId
    )
    interaction.respondEphemeral {
        content = "${Emojis.whiteCheckMark} Thanks for verifying with us. You've finished your signup!"
    }
}