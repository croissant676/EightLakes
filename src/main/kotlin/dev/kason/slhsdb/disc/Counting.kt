package dev.kason.slhsdb.disc

import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.kord
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.kordLogger
import dev.kord.core.on
import kotlinx.coroutines.flow.toList

private var losingCountingSnowflake: Snowflake? = null
private const val searchRoleName: String = "L Counting"

suspend fun checkGuildSaveUsed() {
    val guild = kord.getGuild(guildId)!!
    for (currentRole in guild.roles.toList()) {
        if (currentRole.name == searchRoleName) {
            losingCountingSnowflake = currentRole.id
            break
        }
    }
    if (losingCountingSnowflake == null) losingCountingSnowflake = guild.createRole {
        name = searchRoleName
        color = Color(255, 0, 0)
    }.id
    kord.on<MessageCreateEvent> {
        val user = message.author ?: return@on
        if (!user.isBot) return@on
        if ("used **1** guild save" !in message.content) return@on
        val offender = guild.getMember(message.data.mentions.first())
        kordLogger.debug { "Punished ${offender.displayName}#${offender.discriminator} for being bad at counting." }
        offender.addRole(losingCountingSnowflake!!, "Used a guild save in counting.")
        val channel = message.channel
        channel.createMessage(
            offender.mention + " has been temporarily banned from typing in this channel for using a guild save. " +
                    "They can appeal to regain counting access in the ${countingAppealsChannel!!.mention} channel."
        )
    }
}


private var countingAppealsChannel: GuildChannel? = null

suspend fun findCountingAppealsChannel() {
    countingAppealsChannel = kord.getChannelOf(Snowflake(1010320180318048317))
}

