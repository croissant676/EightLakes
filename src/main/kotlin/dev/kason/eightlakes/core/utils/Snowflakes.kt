package dev.kason.eightlakes.core.utils

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.KProperty


interface SnowflakeDelegate<ID : Comparable<ID>> {
    operator fun getValue(entity: Entity<ID>, property: KProperty<*>): Snowflake
    operator fun setValue(entity: Entity<ID>, property: KProperty<*>, snowflake: Snowflake)
}

private val snowflakeDelegateMap: MutableMap<Column<Long>, SnowflakeDelegate<*>> = mutableMapOf()

// Snowflake Delegate impl
@Suppress("UNCHECKED_CAST")
fun <ID : Comparable<ID>> Entity<ID>.snowflakeDelegate(column: Column<Long>): SnowflakeDelegate<ID> =
    snowflakeDelegateMap.getOrPut(column) {
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
                val snowflakeValue = snowflake.long()
                column.setValue(entity, property, snowflakeValue)
            }
        }
    } as SnowflakeDelegate<ID>

fun Snowflake.long() = value.toLong()