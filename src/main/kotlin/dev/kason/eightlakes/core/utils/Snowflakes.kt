package dev.kason.eightlakes.core.utils

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

fun Snowflake.long() = value.toLong()

class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()
    override fun valueFromDB(value: Any): Snowflake = when (value) {
        is Long -> Snowflake(value)
        is ULong -> Snowflake(value)
        else -> error("$value: Values from the database for a snowflake column should only be longs.")
    }

    override fun notNullValueToDB(value: Any): Long = if (value !is Snowflake)
        error("$value: Snowflake columns can only store snowflakes.")
    else value.long()
}

fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType())