package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kord.common.entity.*
import dev.kord.core.entity.*
import dev.kord.core.entity.channel.*
import dev.kord.rest.builder.channel.addRoleOverwrite
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class CourseService(override val di: DI) : DIAware, DiscordEntityService<Course> {

    private val guild: Guild by instance()
    private val discordService: DiscordService by instance()

    suspend fun registerCourse(
        courseName: String,
        simpleName: String,
        roleId: Snowflake? = null,
        channel: Snowflake? = null,
        explicitCourseLevel: CourseLevel? = null,
        category: Category? = null
    ): Course {
        val courseLevel = explicitCourseLevel ?: CourseLevel.findInCourse(courseName)
        return newSuspendedTransaction {
            val role = setupRole(roleId, courseName)
            val textChannel = setupTextChannel(channel, courseName, role, category)
            Course.new {
                this.courseName = courseName
                this.simpleName = simpleName
                this.courseLevel = courseLevel
                this.discordRole = role.id
                this.discordChannel = textChannel.id
            }
        }
    }

    private suspend fun setupRole(roleId: Snowflake?, courseName: String): Role {
        if (roleId != null) {
            return guild.getRoleOrNull(roleId)
                ?: throw IllegalArgumentException("Role does not exist")
        }
        val role = guild.role {
            name = courseName
            color = generateRandomColor()
        }
        return role
    }

    private suspend fun setupTextChannel(
        channelId: Snowflake?,
        courseName: String,
        role: Role,
        category: Category? = null
    ): TextChannel {
        return if (channelId != null) {
            guild.getChannelOrNull(channelId) as? TextChannel
                ?: throw IllegalArgumentException("Channel does not exist")
        } else discordService.textChannel(courseName.lowercase(), category) {
            addRoleOverwrite(guild.everyoneRole.id) {
                denied = Permissions(Permission.ViewChannel)
            }
            addRoleOverwrite(role.id) {
                allowed = Permissions(
                    Permission.ViewChannel,
                    Permission.SendMessages,
                    Permission.ReadMessageHistory,
                    Permission.AddReactions,
                    Permission.AttachFiles,
                    Permission.UseExternalEmojis,
                    Permission.CreatePublicThreads,
                    Permission.SendMessagesInThreads,
                    Permission.UseExternalStickers
                )
            }
        }
    }


    override suspend fun get(discordId: Snowflake): Course {
        return newSuspendedTransaction {
            Course.find(Courses.discordChannel eq discordId).first()
        }
    }

    override suspend fun getOrNull(discordId: Snowflake): Course? {
        return newSuspendedTransaction {
            Course.find(Courses.discordChannel eq discordId).firstOrNull()
        }
    }


}