package dev.kason.eightlakes.discord.coursereg

import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.core.suspendTransaction
import dev.kason.eightlakes.discord.member
import dev.kord.core.entity.channel.TextChannel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private val headerText = """
    we've created this channel for you to input your courses!
    
    For each period, you'll need to type the full name of the course. 
    For example: `AP Statistics` instead of `Stats`.
    If you need any help, contact one of the admins.
""".trimIndent()

suspend fun allCourseText(): String {
    val courses = newSuspendedTransaction { Course.all().toList() }
    return courses.joinToString(
        prefix = "```\n",
        postfix = "```",
        separator = "\n"
    ) { it.courseName }
}

suspend fun TextChannel.initialize(student: Student) {
    createMessage("Hey, ${student.member().mention}, $headerText")
    createMessage(allCourseText())
    val registration = suspendTransaction {
        Registration.new {
            this.channel = this@initialize.id
            this.student = student.discordId

        }
    }
}