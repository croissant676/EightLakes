package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kason.eightlakes.students.*
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.*
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.channel.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*


// Not supposed to be an actual controller,
// but needs to use controller utilities.
class RegistrationService(override val di: DI) : DiscordController(di),
    DIAware, DiscordEntityService<Registration> {

    private val studentService: StudentService by instance()
    private val classService: ClassService by instance()

    override suspend fun loadCommands() {
        // No commands
    }

    suspend fun createRegistration(
        discordUser: Member
    ): Registration {
        val possibleRegistration = getRegistration(discordUser)
        if (possibleRegistration != null) {
            warn(possibleRegistration, discordUser.id)
            return possibleRegistration
        }
        val student = studentService.getOrNull(discordUser.id)
            ?: throw IllegalArgumentException("${discordUser.mention} you are not signed up. Please sign up using the command `signup`.")
        val channel = discordService.textChannel(
            "registration-${student.studentId}",
            discordService.category("registrations") {
                this.name = "registrations"
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
        val registration = newSuspendedTransaction {
            Registration.new {
                this.student = student
                this.channel = channel.id
            }
        }
        setup(channel, registration)
        return registration
    }

    private suspend fun warn(registration: Registration, userId: Snowflake) {
        val textChannel = guild.getChannel(registration.channel) as TextChannel
        textChannel.createMessage {
            content = "<@$userId> Register your courses here."
        }
    }

    private suspend fun setup(textChannel: TextChannel, registration: Registration) = coroutineScope {
        launch(Dispatchers.IO) {
            newSuspendedTransaction {
                textChannel.createMessage {
                    content = "Welcome to the registration process, ${registration.student.preferredOrFirst}!"
                }
            }
            textChannel.createMessage {
                content =
                    "Please select the courses you would like to register for. Only select the classes that match your teacher."
            }
            val message = textChannel.createMessage {
                with(classService) {
                    this@createMessage.courseDisplayEmbed()
                }
            }
            message.pin()
        }
    }

    suspend fun addCourse(
        discordUser: Member,
        courseClassId: Snowflake
    ): Registration {
        val registration = getRegistration(discordUser)
            ?: throw IllegalArgumentException("${discordUser.mention} you are not registered. Please register using the command `register`.")
        val courseClass = classService.get(courseClassId)
        require(courseClass.period == registration.period) {
            "${dev.kord.x.emoji.Emojis.x} Course class is not in the same period as the registration state"
        }
        registration += courseClass
        if (registration.isFinished) withContext(Dispatchers.IO) {
            launch {
                pushRegistration(registration)
                addRoles(registration, discordUser)
            }
        }
        return registration
    }

    private suspend fun addRoles(registration: Registration, discordUser: Member) = newSuspendedTransaction {
        val classes = getCourseClasses(registration)
        for (courseClass in classes) {
            discordUser.addRole(courseClass.discordRole)
            discordUser.addRole(courseClass.course.discordRole)
            courseClass.teacher.role?.let { discordUser.addRole(it) }
            // transaction in case course and teacher aren't loaded
        }
    }

    @NeedsTransaction
    private fun getCourseClasses(registration: Registration): Set<CourseClass> {
        return registration.classes.map { CourseClass[it] }.toSet()
    }

    @NeedsTransaction
    private suspend fun pushRegistration(registration: Registration): List<StudentClass> {
        check(registration.isFinished) { "Registration is not finished." }
        val result = mutableListOf<StudentClass>()
        newSuspendedTransaction {
            for (id in registration.classes) {
                result += createStudentClass(registration.student, CourseClass[id])
            }
        }
        return result
    }

    @NeedsTransaction
    private fun createStudentClass(student: Student, courseClass: CourseClass): StudentClass {
        return StudentClass.new {
            this.student = student
            this.courseClass = courseClass
        }
    }

    private suspend fun getRegistration(
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

    suspend fun ensureCorrectChannel(channel: GuildChannelBehavior, user: Member) {
        val registration = getRegistration(user)!!
        require(registration.channel == channel.id) {
            "You are not in the correct channel. Please go to <#${registration.channel}>."
        }
    }

    suspend fun deleteRegistration(registration: Registration, studentDiscordId: Snowflake) {
        val channel = guild.getChannelOf<TextChannel>(registration.channel)
        channel.createMessage {
            content = "<@${studentDiscordId}> Deleting registration."
        }
        coroutineScope {
            launch(Dispatchers.IO) {
                delay(3000)
                registration.delete()
                channel.delete()
            }
        }
    }
}
