package dev.kason.slhsdb.core

import dev.kason.slhsdb.guildId
import dev.kason.slhsdb.kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq

suspend fun updateStudentJoinValues() {
    val membersByJoinDate =
        kord.getGuild(guildId)!!.members
            .toList()
            .filterNot { it.isBot }
            .sortedBy { it.joinedAt.epochSeconds }
    membersByJoinDate.forEachIndexed { number, member ->
        val student = studentDatabase.findOneById(Student::discordId eq member.id)
            ?: return@forEachIndexed
        student.memberNumber = number + 1
        studentDatabase.updateOneById(student.studentId, student)
    }
}

suspend fun setUpdateStudentJoinValuesOnJoinOrLeave() {
    kord.on<MemberJoinEvent> {
        updateStudentJoinValues()
    }
    kord.on<MemberLeaveEvent> {
        updateStudentJoinValues()
    }
}
