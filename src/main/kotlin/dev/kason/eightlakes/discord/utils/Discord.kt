package dev.kason.eightlakes.discord.utils

import dev.kason.eightlakes.core.models.Student
import dev.kason.eightlakes.guild
import dev.kason.eightlakes.guildId
import dev.kason.eightlakes.kord
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Member
import dev.kord.core.entity.User

suspend fun User.member(): Member = fetchMember(guildId)
suspend fun User.memberOrNull(): Member? = fetchMemberOrNull(guildId)

suspend fun User.hasAdminPerms(): Boolean = (Permission.Administrator in member().getPermissions())

suspend fun Student.user(): User? = kord.getUser(discordId)
suspend fun Student.member(): Member = guild.getMember(discordId)
suspend fun Student.memberOrNull(): Member? = guild.getMemberOrNull(discordId)