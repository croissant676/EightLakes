package dev.kason.eightlakes.discord.coursereg

import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.discord.member
import dev.kord.core.entity.channel.TextChannel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private const val headerText = """
    This channel will be where you register your classes.
    
    We'll ask you what class you have for a given period, and you just need to type the full name of the class.
    If one of your classes is *not* registered, contact an admin.
"""

suspend fun allCourseText(): String {
    val courses = newSuspendedTransaction {
        Course.all().toList()
    }
    return courses.joinToString(
        prefix = "```\n",
        postfix = "```",
        separator = "\n"
    ) {
        "${it.id} - ${it.courseName}"
    }
}

suspend fun TextChannel.initialize(student: Student) {
    createMessage("Hey, ${student.member().mention}! $headerText")
    createMessage(allCourseText())

}