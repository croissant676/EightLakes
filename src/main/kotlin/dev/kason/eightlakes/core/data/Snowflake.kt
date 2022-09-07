package dev.kason.eightlakes.core.data

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.vendors.currentDialect

// Custom column type that stores snowflake values in the database as longs
private class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()
    override fun valueFromDB(value: Any): Snowflake = when (value) {
        is Snowflake -> value
        is ULong -> Snowflake(value)
        is Long -> Snowflake(value)
        is String -> Snowflake(value)
        is Instant -> Snowflake(value)
        is Number -> Snowflake(value.toLong())
        else -> error("Unexpected value of type Snowflake $value of ${value::class.qualifiedName}")
    }

    // Converting a snowflake.value to a long is fine: check out Snowflake's serializer
    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue = if (value is Snowflake) value.value.toLong() else value
        super.setParameter(stmt, index, parameterValue)
    }

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Snowflake -> value.value.toLong()
        is ULong -> value.toLong()
        is Long -> value
        is String -> value.toULong().toLong()
        else -> value
    }
}

fun Table.snowflake(name: String): Column<Snowflake> =
    registerColumn(name, SnowflakeColumnType())