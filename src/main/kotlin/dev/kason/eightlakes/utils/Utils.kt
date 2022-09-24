@file:Suppress("unused")

package dev.kason.eightlakes.utils

import dev.kord.core.entity.User
import kotlinx.datetime.*

fun String.capitalize() = split(" ").joinToString {
    lowercase().replaceFirstChar { it.uppercase() }
}

fun String.capitalizeLowercase() = split(" ").joinToString {
    replaceFirstChar { it.uppercase() }
}

// Pattern MM/dd/yyyy
fun String.localDate(): LocalDate = try {
    val tokens = split("/")
    LocalDate(
        monthNumber = tokens[0].toInt(),
        dayOfMonth = tokens[1].toInt(),
        year = tokens[2].toInt()
    )
} catch (exception: Exception) {
    throw IllegalArgumentException("Could not parse string $this into a date.", exception)
}

fun LocalDate.toFormattedString() = "$monthNumber/$dayOfMonth/$year"

// 5:03:20.232 PM
// hour:minute:second.millisecond AM/PM
// fill with 0s if less than 10
fun LocalTime.toFormattedString(): String {
    val isAfternoon = hour >= 12
    val hour = if (hour > 12) hour - 12 else hour
    val minute = if (minute < 10) "0$minute" else minute.toString()
    val second = if (second < 10) "0$second" else second.toString()
    val ms = nanosecond / 1_000_000
    val millisecond = if (ms < 10) "00$ms" else if (ms < 100) "0$ms" else ms.toString()
    return "$hour:$minute:$second.$millisecond ${if (isAfternoon) "PM" else "AM"}"
}

fun LocalDateTime.toFormattedString(): String {
    return "${time.toFormattedString()} - ${date.toFormattedString()}"
}

fun Instant.toFormattedString(): String = toLocalDateTime(TimeZone.currentSystemDefault()).toFormattedString()

fun Int.ordinalString(): String {
    val mod100 = this % 100
    return this.toString() + when {
        mod100 == 11 || mod100 == 12 || mod100 == 13 -> "th"
        mod100 % 10 == 1 -> "st"
        mod100 % 10 == 2 -> "nd"
        mod100 % 10 == 3 -> "rd"
        else -> "th"
    }
}

val User.nameAndDiscriminator: String
    get() = username + discriminator