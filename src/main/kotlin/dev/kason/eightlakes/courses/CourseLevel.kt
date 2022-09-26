package dev.kason.eightlakes.courses

import dev.kord.rest.builder.interaction.StringChoiceBuilder

enum class CourseLevel {
    AP,
    KAP,
    Academic,
    Other;

    companion object {

        fun findInCourse(courseName: String) = when {
            "KAP" in courseName -> KAP
            "AP" in courseName -> AP
            "Aca" in courseName -> Academic
            else -> Other
        }

        operator fun get(index: Int) = values()[index]

        fun StringChoiceBuilder.addCourseLevelValues() {
            values().forEach {
                choice(it.name, it.ordinal.toString())
            }
        }

    }

}