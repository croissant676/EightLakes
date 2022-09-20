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