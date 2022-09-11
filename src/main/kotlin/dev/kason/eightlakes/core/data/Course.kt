package dev.kason.eightlakes.core.data

import dev.kason.eightlakes.core.suspendTransaction
import dev.kason.eightlakes.discord.*
import dev.kason.eightlakes.guild
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.channel.addRoleOverwrite
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

enum class CourseLevel {
    AP, KAP, Academic, Other;
}

object Courses : IntIdTable("courses") {
    val courseName = varchar("name", 255).uniqueIndex()
    val courseLevel = enumeration<CourseLevel>("level")
    val discordRole = snowflake("role_id").nullable()
    val discordChannel = snowflake("channel_id").index().nullable()
}

@Suppress("MemberVisibilityCanBePrivate")
class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var courseName by Courses.courseName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordChannel by Courses.discordChannel
    val classes by CourseClass referrersOn CourseClasses.course

    suspend fun allStudentClasses(): Set<StudentClass> = suspendTransaction {
        this@Course.classes.flatMap { it.students }
    }.toSet()

    suspend fun allStudents(): Set<Student> = suspendTransaction {
        this@Course.classes.flatMap { it.students }.map { it.student }
    }.toSet()

    suspend fun createRole(name: String? = null): Role {
        val role = role {
            this.name = name ?: courseName
        }
        val students = allStudents()
        students.forEach {
            val member = it.member()
            member.addRole(role.id, "Added ${member.displayName} to the class ${courseName}.")
        }
        suspendTransaction {
            this@Course.discordRole = role.id
        }
        return role
    }

    // Removes members with the role that aren't supposed to have it (not registered)
    // but also adds the role to those who are supposed to have it.
    suspend fun initRole(role: Role): Role {
        val students = allStudents()
        val studentDiscordIds = students.map { it.discordId }.toMutableSet()
        guild.members.filter { role.id in it.roleIds }.onEach {
            if (it.id !in studentDiscordIds) it.removeRole(
                role.id,
                "${it.nameWithDiscriminator} was not registered in the course $this."
            ) else studentDiscordIds.remove(it.id)
        }
        studentDiscordIds.map { guild.getMember(it) }.forEach {
            val member = it
            member.addRole(role.id, "Added ${member.displayName} to the class ${courseName}.")
        }
        suspendTransaction {
            this@Course.discordRole = role.id
        }
        return role
    }

    suspend fun role(): Role? = discordRole?.run { guild.getRole(this) }
    suspend fun roleOrCreate() = role() ?: createRole()

    suspend fun createChannel(_name: String? = null): TextChannel {
        val name = _name?.substringAfter("|") ?: courseName
        val category = _name?.substringBefore("|") ?: "School"
        val role = roleOrCreate()
        val channel = channel(name, category) {
            addRoleOverwrite(guild.everyoneRole.id) {
                denied = Permissions { +Permission.ViewChannel }
            }
            addRoleOverwrite(role.id) {
                allowed = studentPermissions
            }
        }
        suspendTransaction {
            discordChannel = channel.id
        }
        return channel
    }

    suspend fun initChannel(channel: TextChannel): TextChannel {
        val role = roleOrCreate()
        channel.edit {
            addRoleOverwrite(guild.everyoneRole.id) {
                denied = Permissions { +Permission.ViewChannel }
            }
            addRoleOverwrite(role.id) {
                allowed = studentPermissions
            }
        }
        suspendTransaction {
            discordChannel = channel.id
        }
        return channel
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Course

        if (courseName != other.courseName) return false
        if (courseLevel != other.courseLevel) return false
        if (discordRole != other.discordRole) return false
        if (discordChannel != other.discordChannel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = courseName.hashCode()
        result = 31 * result + courseLevel.hashCode()
        result = 31 * result + (discordRole?.hashCode() ?: 0)
        result = 31 * result + (discordChannel?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Course(courseName='$courseName', courseLevel=$courseLevel, discordRole=$discordRole, discordChannel=$discordChannel)"
    }


}