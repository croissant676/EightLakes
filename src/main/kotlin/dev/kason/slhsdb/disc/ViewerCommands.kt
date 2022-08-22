package dev.kason.slhsdb.disc

import dev.kason.slhsdb.core.*
import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.kord
import dev.kason.slhsdb.onExecute
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.first

private val camelRegex = """([a-z])([A-Z]+)""".toRegex()

suspend fun addViewerCommands() {
    kord.createGuildChatInputCommand(
        guildId,
        name = "course",
        description = "see course"
    ) {
        string("name", "The name of the course. ") {
            courseDatabase.find().toList().forEach {
                choice(it.simpleName, it.simpleName)
            }
            required = false
        }
        string("id", "The database id of the course.") {
            required = false
        }
        string("period", "The period that you have this course.") {
            Period.values().forEach {
                choice(it.formalName, it.formalName)
            }
            required = false
        }
    }.onExecute {
        val response = interaction.deferPublicResponse()
        val course = receiveCourse()
        if (course == null) {
            response.respond {
                content = ":x: We couldn't find a course with that name."
            }
            return@onExecute
        }
        response.respond {
            embed {
                createEmbedFromCourse(course)
            }
        }
    }
    val kClass = StudentBotSettings::class
    kord.createGuildChatInputCommand(
        guildId,
        name = "settings",
        description = "Change personal settings"
    ) {
        string("setting", "Setting to change") {
            kClass.members.filter { name != "forstudent" && name != "settings" }.forEach {
                choice(it.name, it.name)
            }
            required = true
        }
        boolean("value", "The new value.") {
            required = true
        }
    }.onExecute {
        ensureVerified().tap {
            val setting = interaction.command.strings["setting"]
            val value = interaction.command.booleans["value"]
            it.settings.changeSetting(setting!!, value!!)
        }
    }

    //profile
    kord.createGuildChatInputCommand(
        guildId,
        name = "profile",
        description = "See your profile"
    ).onExecute {
        ensureVerified().tap {
            val response = interaction.deferPublicResponse()
            response.respond {
                embed {
                    title = "${it.firstName}\'s Profile"
                    description = it.botData.description
                    color = interaction.user.roles.first().color
                    thumbnail {
                        url = interaction.user.avatar?.url ?: interaction.user.defaultAvatar.url
                    }
                    footer {
                        text = "${it.memberNumber.toOrdinal()} member"
                    }
                }
            }
        }
    }

}


suspend fun EmbedBuilder.createEmbedFromCourse(course: Course) {
    title = course.simpleName
    description = buildString {
        append(course.courseLevel.name).append(" course -")
        append("\n")
        val teachers = course.teachers
        if (teachers != null) {
            append("Taught by ")
            teachers.map { teacherDatabase.findOneById(it)!! }.joinTo(this) {
                it.teacherName() + "[${it.email}]"
            }
        }
    }
    footer {
        text = "#: \"${course.courseId}\""
    }
}
