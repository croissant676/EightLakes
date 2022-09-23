package dev.kason.eightlakes.utils

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

fun LocalTime.toFormattedString() {
    val hour = if (hour > 12) hour - 12 else hour
    val amPm = if (hour > 12) "PM" else "AM"
    "$hour:$minute:$second.$nanosecond $amPm"
}

fun LocalDateTime.toFormattedString(): String {
    val time = time
    val date = date
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