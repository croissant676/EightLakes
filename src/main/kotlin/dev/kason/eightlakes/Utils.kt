@file:Suppress("unused")

package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.vendors.currentDialect
import org.kodein.di.*

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


interface ModuleProducer {

    suspend fun createModule(config: Config): DI.Module

}

open class ConfigAware(
    override val di: DI
) : DIAware {

    // lazy init because referencing di may result in issues:
    // see: https://stackoverflow.com/questions/50222139/kotlin-calling-non-final-function-in-constructor-works
    val config: Config by lazy(LazyThreadSafetyMode.NONE) { di.direct.instance() }

}

class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()
    override fun valueFromDB(value: Any): Snowflake = when (value) {
        is Snowflake -> value
        is Long -> Snowflake(value)
        is ULong -> Snowflake(value)
        is String -> Snowflake(value)
        else -> error("Could not convert $value into a snowflake.")
    }

    override fun notNullValueToDB(value: Any): Long = when (value) {
        is Long -> value
        is ULong -> value.toLong()
        is Snowflake -> value.value.toLong()
        is String -> value.toULong().toLong()
        else -> error("Could not convert $value into a long.")
    }
}

fun Table.snowflake(name: String): Column<Snowflake> =
    registerColumn(name, SnowflakeColumnType())