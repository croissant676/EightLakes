package dev.kason.eightlakes.discord

import dev.kason.eightlakes.config
import dev.kason.eightlakes.core.utils.kColor
import dev.kason.eightlakes.guild
import dev.kason.eightlakes.kord
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList

private var _countingLoserRole: Role? = null
val countingLoserRole: Role get() = _countingLoserRole!!

private var _countingChannel: TextChannel? = null
private val countingChannel: TextChannel get() = _countingChannel!!

private var _countingAppealsChannel: TextChannel? = null
private val countingAppealsChannel: TextChannel get() = _countingAppealsChannel!!

private val countingBotId = Snowflake(config.getString("bot.counting.id"))
private const val countingDetectionMessage = "used **1** guild save"

suspend fun checkCounting() {
    _countingChannel = searchTextChannels("counting") {
        topic = "If you committed a counting crime, this is your place to beg for mercy."
    }
    _countingLoserRole = searchRoles {
        name = "Counting Loser"
        color = kColor("#ef4142")
    }
    kord.on<MessageCreateEvent> {
        val author = message.author ?: return@on
        if (!author.isBot && author.id != countingBotId) return@on
        if (countingDetectionMessage in message.content) {
            val sender = message.mentionedUsers.toList().first()
            val member = guild.getMember(sender.id)
            member.addRole(countingLoserRole.id, "Used a guild save")
            message.channel.createMessage(
                "${member.mention} has been temporarily banned from the counting channel. " +
                        "Please visit the ${countingAppealsChannel.mention} channel to regain counting access."
            )
        }
    }
}