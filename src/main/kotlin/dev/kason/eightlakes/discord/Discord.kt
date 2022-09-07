package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kason.eightlakes.core.Student
import dev.kord.core.entity.Member
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlin.system.measureTimeMillis

@Deprecated(message = "Use Student#member to get a Member, which extends User", ReplaceWith("member()"))
suspend fun Student.user() = kord.getUser(discordId) ?: error("Student <$id> has improper discord id.")
suspend fun Student.member() = guild.getMember(discordId) // Use member for everything

fun Member.name() = "$displayName#$discriminator"

val GuildAppCommandEvent.member get() = interaction.user

private val _serverOrderMap = hashMapOf<Member, Int>()
private val orderMapInitialized: Boolean get() = _serverOrderMap.isNotEmpty()

suspend fun Member.order(): Int {
    if (!orderMapInitialized) generateServerOrderMap()
    return _serverOrderMap[this] ?: error("Member $displayName")
}

private suspend fun generateServerOrderMap() = withContext(Dispatchers.IO) {
    logger.info { "Generating server order // done before: $orderMapInitialized" }
    val time = measureTimeMillis {
        _serverOrderMap.clear()
        val members = guild.members.toList().sortedBy { it.joinedAt }
        _serverOrderMap += members.mapIndexed { number, member ->
            member to number
        }
    }
    logger.info { "Finished generating server order in $time ms." }
}