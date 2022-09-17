package dev.kason.eightlakes.discord.coursereg

import dev.kason.eightlakes.discord.*
import dev.kord.common.Color
import dev.kord.core.entity.Role

private var _registeredRole: Role? = null
suspend fun registeredRole(): Role = _registeredRole ?: role {
    this.name = "course registered"
    this.color = Color(30, 144, 255)
}.also { _registeredRole = it }

suspend fun _registrationCommand() = chatInputCommand(
    "register",
    "Register your courses."
) {

}.onExecute {

}