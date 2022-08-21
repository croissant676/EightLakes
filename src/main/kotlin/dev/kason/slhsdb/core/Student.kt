package dev.kason.slhsdb.core

import dev.kason.slhsdb.Base64Id
import dev.kason.slhsdb.database
import dev.kason.slhsdb.randomId
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import org.litote.kmongo.eq

@kotlinx.serialization.Serializable
class Student(
    @SerialName("_id")
    val studentId: Base64Id = randomId(),
    var discordId: Snowflake,
    var firstName: String,
    var middleName: String?,
    var lastName: String,
    var preferredName: String?,
    var studentCode: String,
    var birthday: LocalDate? = null,
    var courses: MutableMap<Period, StudentCourse> = mutableMapOf(),
    var genderSelectionOption: GenderSelectionOption? = null,
    var botData: StudentBotData = StudentBotData(studentId),
    var settings: StudentBotSettings = StudentBotSettings(studentId),
    var memberNumber: Int
) {
    val gender: Gender? get() = genderSelectionOption?.gender
}

val studentDatabase = database.getCollection<Student>()

@kotlinx.serialization.Serializable
class StudentCourse(
    val courseId: Base64Id = randomId(),
    var studentId: Base64Id,
    var teacher: String? = null,
    var period: Period,
) {
    suspend fun course(): Course? = courseDatabase.findOneById(courseId)
    suspend fun student(): Student? = studentDatabase.findOneById(studentId)
}

@kotlinx.serialization.Serializable
data class StudentBotData(
    val forStudent: Base64Id,
    var description: String = "A Seven Lakes student.",
    var userJoinDate: LocalDate? = null,
    var userJoinValue: Int? = null,
)

val defaultSettings: MutableMap<String, Boolean> = mutableMapOf(
    "enabledDailyReminders" to false,
    "enabledBirthdayPing" to true
)

@kotlinx.serialization.Serializable
data class StudentBotSettings(
    val forStudent: Base64Id,
    val settings: MutableMap<String, Boolean> = defaultSettings.toMutableMap()
) {
    val enabledDailyReminders: Boolean by settings
    val enabledBirthdayPing: Boolean by settings

    fun changeSetting(setting: String, enabled: Boolean) {
        if (!settings.containsKey(setting)) throw IllegalStateException("Invalid setting: $setting")
        else settings[setting] = enabled
    }
}

suspend fun student(discordId: Snowflake) = studentDatabase.findOne(Student::discordId eq discordId)