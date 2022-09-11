package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.*
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.first

private val blocks: MutableSet<SuspendingExecution<MessageCreateEvent>> = mutableSetOf()

suspend fun registerMessageListener() = kord.on<MessageCreateEvent> { blocks.forEach { it(this) } }

fun onMessageCreate(block: SuspendingExecution<MessageCreateEvent>) {
    logger.debug("Added new onMessageCreate() block.")
    blocks += block
}

private var _countingLoserRole: Role? = null

private val countingId = Snowflake(config.getLong("bot.counting-bot"))
private val channelId = Snowflake(config.getLong("bot.counting-channel"))
private val channelAppealsId = Snowflake(config.getLong("bot.counting-appeals"))

suspend fun countingLoser(): Role {
    if (_countingLoserRole != null) return _countingLoserRole!!
    _countingLoserRole = role {
        name = "Counting Loser"
        color = Color(255, 26, 64)
    }
    return _countingLoserRole!!
}

// When someone uses a guild save, prohibits them from counting and forces them into appeals.
suspend fun registerCountingBotListener() {
    val countingLoserRole = countingLoser()
    val countingChannel = guild.getChannel(channelId) as TextChannel
    val countingAppealsChannel = guild.getChannel(channelAppealsId) as TextChannel
    countingAppealsChannel.edit {
        addRoleOverwrite(countingLoserRole.id) {
            allowed = defaultPermissions
        }
        addRoleOverwrite(guild.everyoneRole.id) {
            denied = Permissions(Permission.ViewChannel)
        }
    }
    countingChannel.edit {
        addRoleOverwrite(countingLoserRole.id) {
            denied = Permissions(Permission.SendMessages)
        }
    }
    onMessageCreate {
        if (this.message.channelId != channelId) return@onMessageCreate // Only counting channel
        val author = message.author
        if (author == null || !author.isBot) return@onMessageCreate // We want only the messages from counting bot.
        if (author.id != countingId) return@onMessageCreate
        if ("You have used **1** guild save!" !in message.content) return@onMessageCreate
        val offender = message.mentionedUsers.first().asMember(guild.id)
        offender.addRole(countingLoserRole.id, "Used a guild save.")
        countingChannel.createMessage {
            content =
                "${Emojis.x} ${offender.mention} used a guild save and has been prohibited from sending messages in this channel. " +
                        "They can go appeal this in ${countingAppealsChannel.mention}."
        }
        logger.warn { "Detected counting offender ${offender.nameWithDiscriminator} and has given them the counting loser role." }
    }
    logger.debug { "Registered counting bot listener." }
}