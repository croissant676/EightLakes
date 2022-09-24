package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.entity.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class TeacherService(override val di: DI) : DIAware, DiscordEntityService<Teacher> {
    private val guild: Guild by instance()

    override suspend fun getOrNull(discordId: Snowflake) = newSuspendedTransaction {
        Teacher.find { Teachers.role eq discordId }.firstOrNull()
    }

    override suspend fun get(discordId: Snowflake) = requireNotNull(getOrNull(discordId)) {
        "Teacher with role ${guild.getRole(discordId)} not found"
    }

    suspend fun createTeacher(
        firstName: String,
        middleName: String?,
        lastName: String,
        email: String
    ): Teacher {
        val capitalizedFirst = firstName.capitalized()
        val capitalizedMiddle = middleName?.capitalized()?.first()
        val capitalizedLast = lastName.capitalized()
        val newEmail = email.substringBefore("@").lowercase()
        require(newSuspendedTransaction { Teacher.find { Teachers.email eq newEmail }.empty() }) {
            "Email $email already exists"
        }
        val teacher = newSuspendedTransaction {
            val role = createRoleFor(firstName, middleName, lastName)
            Teacher.new {
                this.firstName = capitalizedFirst
                this.middleInitial = capitalizedMiddle
                this.lastName = capitalizedLast
                this.email = newEmail
                this.role = role.id
            }
        }
        return teacher
    }

    private suspend fun createRoleFor(
        firstName: String,
        middleInitial: String?,
        lastName: String
    ): Role {
        val name = "$firstName ${middleInitial?.let { "$it. " } ?: ""}$lastName"
        val role = guild.role {
            this.name = name
            this.mentionable = true
            this.color = generateRandomColor()
        }
        return role
    }

    suspend fun useExistingRole(role: Role, teacher: Teacher): Role {
        role.edit {
            this.mentionable = true
            this.name = teacher.fullName
        }
        return role
    }

    // If the teacher has any course classes, it is "dangerous" to delete them
    // because cascading is enabled.
    suspend fun isSafeToDelete(teacher: Teacher): Boolean = newSuspendedTransaction {
        teacher.courseClasses.empty()
    }

    suspend fun deleteTeacher(teacher: Teacher) {
        guild.getRole(teacher.role!!).delete()
        newSuspendedTransaction {
            teacher.delete()
        }
    }


}