package dev.kason.eightlakes.utils

import kotlinx.datetime.LocalDate

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