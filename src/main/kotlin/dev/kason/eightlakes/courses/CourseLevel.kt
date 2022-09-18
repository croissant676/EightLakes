package dev.kason.eightlakes.courses

enum class CourseLevel {
    AP,
    KAP,
    Academic,
    Other;

    companion object {
        fun parse(courseName: String) = when {
            "KAP" in courseName -> KAP
            "AP" in courseName -> AP
            "Aca" in courseName -> Academic
            else -> Other
        }
    }

}