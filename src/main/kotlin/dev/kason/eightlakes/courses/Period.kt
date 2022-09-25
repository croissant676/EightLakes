package dev.kason.eightlakes.courses

import dev.kason.eightlakes.ordinalString

enum class Period {
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh;

    val number: Int
        get() = ordinal + 1

    val ordinalString: String
        get() = number.ordinalString()

    val next: Period?
        get() = values().getOrNull(ordinal + 1)
    val previous: Period?
        get() = values().getOrNull(ordinal - 1)

    companion object {

        private val stringMap = buildMap {
            values().forEach {
                put(it.ordinalString, it)
                put(it.name.lowercase(), it)
                put(it.number.toString(), it)
            }
        }

        fun parse(string: String): Period? = stringMap[string.lowercase()]
    }
}