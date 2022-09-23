package dev.kason.eightlakes.courses

import dev.kason.eightlakes.utils.ordinalString

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