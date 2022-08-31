package dev.kason.eightlakes.discord

import dev.kason.eightlakes.guild
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Role
import dev.kord.rest.builder.role.RoleCreateBuilder
import kotlinx.coroutines.flow.toList

suspend fun searchRoles(
    block: RoleCreateBuilder.() -> Unit = {}
): Role {
    val roleCreateBuilder = RoleCreateBuilder().apply(block)
    val roles = guild.roles.toList()
    // Having the same color, name, and permissions should be enough
    return roles.firstOrNull {
        it.name == roleCreateBuilder.name
    } ?: guild.createRole(block)
}

private var _adminRole: Role? = null
val adminRole: Role get() = _adminRole!!

suspend fun searchForAdminRole() {
    searchRoles {
        name = "admin"
    }
}