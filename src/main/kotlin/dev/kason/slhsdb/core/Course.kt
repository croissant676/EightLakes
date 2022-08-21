package dev.kason.slhsdb.core

import dev.kason.slhsdb.Base64Id
import dev.kason.slhsdb.database
import dev.kason.slhsdb.disc.toOrdinal
import dev.kason.slhsdb.randomId
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
class Course(
    @SerialName("_id")
    var courseId: Base64Id = randomId(),
    var simpleName: String,
    val teachers: MutableList<Base64Id>? = mutableListOf(),
    val students: MutableList<StudentCourse> = mutableListOf(),
    var courseLevel: CourseLevel = CourseLevel.Other,
    var channel: Snowflake? = null,
    val assignments: MutableList<Base64Id> = mutableListOf()
)

val courseDatabase = database.getCollection<Course>()

@kotlinx.serialization.Serializable
enum class Period {
    FirstPeriod,
    SecondPeriod,
    ThirdPeriod,
    FourthPeriod,
    FifthPeriod,
    SixthPeriod,
    SeventhPeriod;

    val number get() = ordinal + 1
    val formalName: String get() = ordinal.toOrdinal()
}

fun parsePeriod(inputString: String): Period = when (inputString) {
    "1st" -> Period.FirstPeriod
    "2nd" -> Period.SecondPeriod
    "3rd" -> Period.ThirdPeriod
    "4th" -> Period.FourthPeriod
    "5th" -> Period.FifthPeriod
    "6th" -> Period.SixthPeriod
    "7th" -> Period.SeventhPeriod
    else -> throw IllegalStateException()
}

@kotlinx.serialization.Serializable
enum class CourseLevel {
    KAP, AP, Academic, Other;
}

@kotlinx.serialization.Serializable
class Teacher(
    @SerialName("_id")
    var teacherId: Base64Id = randomId(),
    var firstName: String,
    var middleName: String?,
    var lastName: String,
    var email: String,
    val courses: MutableMap<Period, Course>
) {
    fun teacherName() =
        if (middleName != null) "$firstName ${middleName!!.first()}. $lastName"
        else "$firstName $lastName"
}

val teacherDatabase = database.getCollection<Teacher>()

@kotlinx.serialization.Serializable
class Assignment(
    @SerialName("_id")
    val assignmentId: Base64Id = randomId(),
    var name: String,
    var dueDate: LocalDate?,
    var weight: AssignmentWeight?,
    var description: String?,
    val course: Course,
    val teacher: Base64Id?
)

enum class AssignmentWeight {
    Major,
    Minor,
    Other
}

val assignmentDatabase = database.getCollection<Assignment>()