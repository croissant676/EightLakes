package dev.kason.eightlakes.courses

import dev.kason.eightlakes.*
import dev.kason.eightlakes.students.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.hours

object Registrations : IntIdTable("registrations") {
    private const val ClassBinarySize = 28
    private const val UnregisteredClass: Byte = -1
    val student = reference("student", Students)
    val period = enumeration<Period>("period").default(Period.First)
    val classes =
        binary("classes", ClassBinarySize).default(ByteArray(7) { UnregisteredClass }) // 7 periods * 4 bytes / int
    val startTimestamp = timestamp("start_timestamp").defaultExpression(CurrentTimestamp())
    val isPaused = bool("is_paused").default(false)
    val isFinished = bool("is_finished").default(false)
    val channel = snowflake("channel")
}

@Suppress("MemberVisibilityCanBePrivate")
class Registration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Registration>(Registrations) {
        val ExpirationDuration = 1.hours
    }

    var student by Student referencedOn Registrations.student
    var period by Registrations.period
    var rawClasses by Registrations.classes
    var startTimestamp by Registrations.startTimestamp
    var isPaused by Registrations.isPaused
    var isFinished by Registrations.isFinished
    var channel by Registrations.channel

    val classes: IntArray by lazy { rawClasses.toIntArray() }

    val isExpired: Boolean
        get() = !isPaused && startTimestamp + ExpirationDuration > Clock.System.now()

    suspend operator fun plusAssign(courseClass: CourseClass) = newSuspendedTransaction {
        classes[period.ordinal] = courseClass.id.value
        // convert classes into a byte array and set rawClasses to that
        // raw classes is size 28
        rawClasses = classes.toByteArray()
        if (period != Period.Seventh) {
            period = period.next!!
        }
    }
}