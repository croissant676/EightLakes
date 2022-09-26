package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kason.eightlakes.students.*
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.channel.*
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*
import kotlin.random.Random


// Not supposed to be an actual controller,
// but needs to use controller utilities.
class RegistrationService(override val di: DI) : DiscordController(di),
    DIAware, DiscordEntityService<Registration> {

    private val studentService: StudentService by instance()

    override suspend fun loadCommands() {
        // No initialization; should be service.
    }

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
            setup(channel, registration)
            registration
        }
    }

    private suspend fun setup(textChannel: TextChannel, registration: Registration) {
        textChannel.createMessage {
            content = "Welcome to the registration process for ${registration.student.preferredName}!"
        }
        textChannel.createMessage {
            content =
                "Please select the courses you would like to register for. Only select the classes that match your teacher."
        }
        val message = textChannel.createMessage {
            sendRegistrationEmbedMessage(0, fetch(0))
        }
        message.pin()
    }

    private suspend fun EmbedBuilder.createCourseRegistrationEmbed(page: Int, list: List<CourseClass>) {
        title = "Courses"
        newSuspendedTransaction {
            for (item in list) {
                field {
                    name = item.course.courseName
                    value = item.teacher.fullName
                }
            }
        }
        footer {
            text = "Page ${page + 1} of ${list.size / 10 + 1}"
        }
    }

    private suspend fun fetch(page: Int): List<CourseClass> {
        return newSuspendedTransaction {
            CourseClass.all().limit(10, (page * 10).toLong()).toList()
        }
    }

    private suspend fun MessageCreateBuilder.sendRegistrationEmbedMessage(currentPage: Int, list: List<CourseClass>) {
        embed {
            createCourseRegistrationEmbed(currentPage, list)
        }
        actionRow {
            button("move-left-${Random.nextBytes(15).encodeBase64()}") {
                label = "<- Previous"
                disabled = (currentPage == 0)
            }.onExecute {
                val response = interaction.deferPublicMessageUpdate()
                // do some calculations to get embed for currentPage - 1
                val newList = fetch(currentPage - 1)
                response.edit {
                    sendRegistrationEmbedMessage(currentPage - 1, newList)
                }
            }
            val rightButtonDisabled =
                (list.size != 10) && (newSuspendedTransaction { CourseClass.all().count() } <= (currentPage + 1) * 10)
            button("move-right-${Random.nextBytes(15).encodeBase64()}") {
                label = "Next ->"
                disabled = rightButtonDisabled
            }.onExecute {
                val response = interaction.deferPublicMessageUpdate()
                val newList = fetch(currentPage + 1)
                response.edit {
                    sendRegistrationEmbedMessage(currentPage + 1, newList)
                }
            }
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
