package dev.kason.eightlakes.discord

import dev.kason.eightlakes.guild
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.channel.TextChannelCreateBuilder
import dev.kord.rest.builder.channel.VoiceChannelCreateBuilder
import kotlinx.coroutines.flow.toList

suspend fun searchTextChannels(
    name: String,
    block: TextChannelCreateBuilder.() -> Unit = {}
): TextChannel {
    val textChannels = guild.channels.toList()
    return (textChannels.firstOrNull {
        it is TextChannel && name in it.name
    } ?: guild.createTextChannel(name, block)) as TextChannel
}

suspend fun searchVoiceChannels(
    name: String,
    block: VoiceChannelCreateBuilder.() -> Unit = {}
): VoiceChannel {
    val guildChannels = guild.channels.toList()
    val voiceChannel = guildChannels.firstOrNull {
        name in it.name
    }
    return voiceChannel!! as VoiceChannel
}