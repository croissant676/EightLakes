package dev.kason.slhsdb.disc

import dev.kason.slhsdb.core.Course
import dev.kason.slhsdb.core.Period
import dev.kason.slhsdb.core.courseDatabase
import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.idFromString
import dev.kason.slhsdb.kord
import dev.kason.slhsdb.onExecute
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.first
import org.litote.kmongo.eq

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
        val strings = interaction.command.strings
        val name = strings["name"]
        val response = interaction.deferPublicResponse()
        if (name != null) {
            response.respond {
                val course = courseDatabase.findOne(Course::simpleName eq name)!!
                embed {
                    createEmbedFromCourse(course)
                }
            }
            return@onExecute
        }
        val id = strings["id"]
        if (id != null) {
            response.respond {
                val course = runCatching {
                    courseDatabase.findOneById(idFromString(id))!!
                }.getOrElse {
                    content = ":x: We couldn't parse your id."
                    return@respond
                }
                embed {
                    createEmbedFromCourse(course)
                }
            }
            return@onExecute
        }
        val period = strings["period"]
        if (period != null) {
            ensureVerified().tap {
                response.respond {

                }
            }
        }
    }
    kord.createGuildChatInputCommand(
        guildId,
        name = "settings",
        description = "Change personal settings"
    ) {
        string("setting", "Setting to change") {
            required = true
        }
    }.onExecute {
        print("hi")
    }

    //profile
    kord.createGuildChatInputCommand(
        guildId,
        name = "profile",
        description = "See your profile"
    ).onExecute {
        ensureVerified().tap {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                embed {
                    title = "${it.firstName}\'s Profile"
                    description = it.settings.description
                    color = interaction.user.roles.first().color
                    thumbnail {
                        url = interaction.user.avatar?.url ?: interaction.user.defaultAvatar.url
                    }
                    footer {
                        text = "nth Member"
                    }
                }
            }
        }
    }

}


suspend fun EmbedBuilder.createEmbedFromCourse(course: Course) {

}