package dev.kason.slhsdb.disc

import dev.kason.slhsdb.core.CourseLevel
import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.kord
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string

suspend fun addCourseRegistrationCommands() {
    kord.createGuildChatInputCommand(
        guildId,
        "regcourse",
        "Creates a course with the given specifications."
    ) {
        string("name", "The full name of the class. For example: 'AP Statistics'") {
            required = true
        }
        string("type", "The course level of the class. ") {
            required = true
            CourseLevel.values().forEach { choice(it.name, it.name) }
        }
        channel("channel", "The channel in which the course ") {

        }
    }
}