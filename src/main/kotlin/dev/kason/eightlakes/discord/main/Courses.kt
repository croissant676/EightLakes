package dev.kason.eightlakes.discord.main

import dev.kason.eightlakes.core.*
import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.discord.*
import dev.kord.common.entity.*
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.channel.*
import dev.kord.rest.builder.interaction.*
import dev.kord.x.emoji.Emojis
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Suppress("SpellCheckingInspection")
suspend fun _adminCourseCommands() = chatInputCommand(
    "courses",
    "Modify, add, or remove courses.",
) {
    subCommand("create", "Creates a new course with the given arguments.") {
        string(
            "name", "The name of the course. For instance, `AP Statistics` or `KAP Precalculus`.",
            dev.kason.eightlakes.discord.required
        )
        string("level", "The course level. Must be one of the choices. For ex, `AP Statistics` would be `AP`.") {
            required = false
            CourseLevel.values().forEach {
                choice(it.name, it.name)
            }
        }
    }
    subCommand("setrole", "Sets the role to the one specified.") {
        string(
            "course", "The full name of the course. For instance, `AP Statistics` or `Orchestra`",
            dev.kason.eightlakes.discord.required
        )
        role(
            "role", "The role given to the course.",
            dev.kason.eightlakes.discord.required
        )
    }
    subCommand("role", "Creates a new role for the given course.") {
        string(
            "course", "The full name of the course. For instance, `AP Statistics` or `Orchestra`",
            dev.kason.eightlakes.discord.required
        )
        string(
            "name", "The name of the role. If not specified, will default to the name of the course.",
            notRequired
        )
    }
    subCommand("setchannel", "Sets the channel to the one specified.") {
        string(
            "course", "The full name of the course. For instance, `AP Statistics` or `Orchestra`",
            dev.kason.eightlakes.discord.required
        )
        channel(
            "channel", "The channel given to the course.",
            dev.kason.eightlakes.discord.required
        )
    }
    subCommand("channel", "Creates a new channel for the given channel.") {
        string(
            "course", "The full name of the course. For instance, `AP Statistics` or `Orchestra`",
            dev.kason.eightlakes.discord.required
        )
        string(
            "name", "The name of the channel. If not specified, will default to the name of the course.",
            notRequired
        )
    }
    subCommand("view", "Returns an embed containing all the courses") {
        int("page", "The page number. Each page is 5 courses.", notRequired)
    }
    defaultMemberPermissions = Permissions(Permission.Administrator)
}.onExecute {
    val subcommand = interaction.command as dev.kord.core.entity.interaction.SubCommand
    when (subcommand.name) {
        "create" -> {
            val name by interaction.command.strings
            val course = registerCourse(
                name,
                interaction.command.strings["level"]
            )
            interaction.respondPublic {
                content =
                    "${Emojis.whiteCheckMark} You created a new course - `${course.courseName}` with id ${course.id}."
            }
        }
        "setrole" -> {
            val role by interaction.command.roles
            val courseName by interaction.command.strings
            val course = suspendTransaction {
                Course.find(Courses.courseName eq courseName).firstOrNull()
            } ?: illegalArg("Could not find a course with name `$courseName`")
            course.initRole(role)
            interaction.respondPublic {
                content =
                    "${Emojis.whiteCheckMark} You set the role for `${course.courseName}` successfully."
            }
        }
        "role" -> {
            val name = interaction.command.strings["name"]
            val courseName by interaction.command.strings
            val course = suspendTransaction {
                Course.find(Courses.courseName eq courseName).firstOrNull()
            } ?: illegalArg("Could not find a course with name `$courseName`")
            course.createRole(name)
            interaction.respondPublic {
                content =
                    "${Emojis.whiteCheckMark} You set the role for `${course.courseName}` successfully."
            }
        }
        "setchannel" -> {
            val channel by interaction.command.channels
            val courseName by interaction.command.strings
            val course = suspendTransaction {
                Course.find(Courses.courseName eq courseName).firstOrNull()
            } ?: illegalArg("Could not find a course with name `$courseName`")
            course.initChannel(Channel.from(channel.data, kord) as TextChannel)
            interaction.respondPublic {
                content =
                    "${Emojis.whiteCheckMark} You set the channel for `${course.courseName}` as ${channel.mention} successfully."
            }
        }
        "channel" -> {
            val name = interaction.command.strings["name"]
            val courseName by interaction.command.strings
            val course = suspendTransaction {
                Course.find(Courses.courseName eq courseName).firstOrNull()
            } ?: illegalArg("Could not find a course with name `$courseName`")
            val channel = course.createChannel(name)
            interaction.respondPublic {
                content =
                    "${Emojis.whiteCheckMark} You set the channel for `${course.courseName}` as ${channel.mention} successfully."
            }
        }
        "view" -> {
            val page = interaction.command.integers["page"] ?: 1
            val range = ((page - 1) * 5 + 1).toInt()..(page * 5).toInt()
            val courses = suspendTransaction {
                Course.forIds(range.toList()).toList()
            }

        }
    }
}