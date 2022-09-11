package dev.kason.eightlakes.core

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private val _range = 11..13

fun Int.englishOrdinal(): String {
    val remainder = this % 100
    val returnString: String = if (remainder in _range) "th"
    else when (remainder % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return remainder.toString() + returnString
}

fun illegalArg(message: String, cause: Throwable? = null): Nothing =
    throw IllegalArgumentException(message, cause)

suspend fun <T> suspendTransaction(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

// Source: https://stackoverflow.com/questions/52042903/capitalise-every-word-in-string-with-extension-function
fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }