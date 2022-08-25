package dev.kason.eightlakes.core

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import kotlin.reflect.KProperty

fun Int.toOrdinal(): String = this.toString() + when (this % 10) {
    1 -> "st"
    2 -> "nd"
    3 -> "rd"
    else -> "th"
}

interface SnowflakeDelegate<ID : Comparable<ID>> {
    operator fun getValue(entity: Entity<ID>, property: KProperty<*>): Snowflake
    operator fun setValue(entity: Entity<ID>, property: KProperty<*>, snowflake: Snowflake)
}

// Snowflake Delegate impl
fun <ID : Comparable<ID>> Entity<ID>.snowflakeDelegate(column: Column<Long>): SnowflakeDelegate<ID> =
    object : SnowflakeDelegate<ID> {
        var cachingSnowflake: Snowflake? = null
        override fun getValue(entity: Entity<ID>, property: KProperty<*>): Snowflake {
            val dataValue = column.lookup().toULong()
            if (cachingSnowflake == null || dataValue != cachingSnowflake!!.value)
                cachingSnowflake = Snowflake(dataValue)
            return cachingSnowflake!!
        }

        override fun setValue(entity: Entity<ID>, property: KProperty<*>, snowflake: Snowflake) {
            // Converting ULong to Long is allowed: look at Snowflake's Serializer
            val snowflakeValue = snowflake.value.toLong()
            column.setValue(entity, property, snowflakeValue)
        }
    }

fun createSqlOp(block: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> =
    SqlExpressionBuilder.block()