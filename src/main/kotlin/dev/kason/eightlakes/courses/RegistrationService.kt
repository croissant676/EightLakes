package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kason.eightlakes.students.*
import dev.kord.common.entity.*
import dev.kord.core.entity.*
import dev.kord.rest.builder.channel.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class RegistrationService(override val di: DI) : DIAware, DiscordEntityService<Registration> {

    private val studentService: StudentService by instance()
    private val courseService: CourseService by instance()
    private val discordService: DiscordService by instance()

    private val guild: Guild by instance()

    suspend fun createRegistration(
        discordUser: Member
    ): Registration {
        return newSuspendedTransaction {
            val student = studentService.getTransactionless(discordUser.id)
                ?: throw IllegalArgumentException("${discordUser.mention} you are not signed up. Please sign up using the command `signup`.")
            val channel = discordService.textChannel(
                "registration-${student.studentId}",
                discordService.category("registrations") {
                    this.name = "Registrations"
                }
            ) {
                topic = "Registration for ${student.fullNameWithPreferredAndMiddleInitial}"
                addRoleOverwrite(guild.everyoneRole.id) {
                    denied = Permissions(Permission.ViewChannel)
                }
                addMemberOverwrite(student.discordId) {
                    allowed = Permissions(
                        Permission.ViewChannel,
                        Permission.SendMessages,
                        Permission.ReadMessageHistory
                    )
                }
            }
            val registration = Registration.new {
                this.student = student
                this.channel = channel.id
            }
            registration
        }
    }

    suspend fun getRegistration(
        discordUser: Member
    ): Registration? {
        return newSuspendedTransaction {
            val student = studentService.getTransactionless(discordUser.id)
                ?: throw IllegalArgumentException("${discordUser.mention} you are not signed up. Please sign up using the command `signup`.")
            Registration.find { Registrations.student eq student.id }.firstOrNull()
        }
    }

    override suspend fun get(discordId: Snowflake): Registration {
        return newSuspendedTransaction {
            Registration.find { Registrations.channel eq discordId }.firstOrNull()
                ?: throw IllegalArgumentException("Registration not found.")
        }
    }

    override suspend fun getOrNull(discordId: Snowflake): Registration? {
        return newSuspendedTransaction {
            Registration.find { Registrations.channel eq discordId }.firstOrNull()
        }
    }

}
