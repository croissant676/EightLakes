package dev.kason.eightlakes.discord.coursereg

import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.discord.member
import dev.kord.core.entity.channel.TextChannel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private val headerText = """
    Register your classes here.
    
    For each period, type the full name of the course. For example: `AP Statistics` instead of `Stats`.
    If one of your classes is *not* listed above, contact an admin.
""".trimIndent()

suspend fun allCourseText(): String {
    val courses = newSuspendedTransaction { Course.all().toList() }
    return courses.joinToString(
        prefix = "```\n",
        postfix = "```",
        separator = "\n"
    ) { "${it.id} - ${it.courseName}" }
}

suspend fun TextChannel.initialize(student: Student) {
    createMessage("Hey, ${student.member().mention}! $headerText")
    createMessage(allCourseText())

}