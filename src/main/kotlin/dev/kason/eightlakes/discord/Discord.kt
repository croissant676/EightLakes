package dev.kason.eightlakes.discord

import dev.kason.eightlakes.*
import dev.kason.eightlakes.core.data.Student
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.createTextChannel
import dev.kord.core.entity.*
import dev.kord.core.entity.channel.*
import dev.kord.rest.builder.channel.*
import dev.kord.rest.builder.role.RoleCreateBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.contracts.*
import kotlin.system.measureTimeMillis

@Deprecated(message = "Use Student#member to get a Member, which extends User", ReplaceWith("member()"))
suspend fun Student.user() = kord.getUser(discordId) ?: error("Student <$id> has improper discord id.")
suspend fun Student.member() = guild.getMember(discordId) // Use member for everything

val Member.nameWithDiscriminator get() = "$displayName#$discriminator"

val GuildAppCommandEvent.member get() = interaction.user

private val _serverOrderMap = hashMapOf<Member, Int>()
private val orderMapInitialized: Boolean get() = _serverOrderMap.isNotEmpty()

suspend fun Member.order(): Int {
    if (!orderMapInitialized) generateServerOrderMap()
    return _serverOrderMap[this] ?: error("Member $nameWithDiscriminator could not be found in the map.")
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

// Returns the role if it already exists or creates a new one.
@OptIn(ExperimentalContracts::class)
suspend fun role(
    block: RoleCreateBuilder.() -> Unit
): Role {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    // because logic may be in the block
    // we only want it to execute once.
    val builder = RoleCreateBuilder().apply(block)
    return guild.roles.firstOrNull {
        it.name == builder.name
    } ?: guild.createRole {
        name = builder.name
        reason = builder.name
        color = builder.color
        hoist = builder.hoist
        icon = builder.icon
        unicodeEmoji = builder.unicodeEmoji
        mentionable = builder.mentionable
        permissions = builder.permissions
    }
}

// Returns the channel if it already exists or creates a new one.
suspend fun channel(
    name: String,
    categoryName: String?,
    block: TextChannelCreateBuilder.() -> Unit = {}
): TextChannel {
    if (categoryName != null) {
        val category = category(categoryName)
        return category.channels.filterIsInstance<TextChannel>().firstOrNull {
            it.name == name
        } ?: category.createTextChannel(name, block)
    }
    return guild.channels.filterIsInstance<TextChannel>()
        .firstOrNull { it.name == name } ?: guild.createTextChannel(name, block)
}

// Returns the category if it already exists or creates a new one.
suspend inline fun category(
    name: String,
    block: CategoryCreateBuilder.() -> Unit = {}
): Category = guild.channels.filterIsInstance<Category>()
    .firstOrNull { it.name == name } ?: guild.createCategory(name, block)