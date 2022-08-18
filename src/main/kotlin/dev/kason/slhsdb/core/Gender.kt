package dev.kason.slhsdb.core

@kotlinx.serialization.Serializable
enum class Gender {
    Male,
    Female,
    Other;
}

@kotlinx.serialization.Serializable
enum class GenderSelectionOption {
    HeHim,
    HeThey,
    TheyThem,
    SheThey,
    SheHer;

    val gender: Gender
        get() = when (this) {
            HeHim -> Gender.Male
            SheHer -> Gender.Female
            else -> Gender.Other
        }
    val text: String
        get() = when (this) {
            HeHim -> "he / him"
            HeThey -> "he / they"
            TheyThem -> "they / them"
            SheThey -> "she / they"
            SheHer -> "she / her"
        }
}