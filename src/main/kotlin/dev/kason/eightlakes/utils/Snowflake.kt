package dev.kason.eightlakes.utils

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.vendors.currentDialect

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