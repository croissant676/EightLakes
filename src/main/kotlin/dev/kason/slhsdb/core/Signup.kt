package dev.kason.slhsdb.core

import arrow.core.Either
import dev.kason.slhsdb.Base64Id
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.LocalDate
import org.litote.kmongo.eq

suspend fun registerStudent(
    discordId: Snowflake,
    firstName: String,
    middleName: String? = null,
    lastName: String,
    studentCode: String,
    preferredName: String?
): Either<StudentRegistrationError, Verification> {
    if (studentDatabase.findOne(Student::discordId eq discordId) != null) {
        return Either.Left(StudentRegistrationError.AccountRegistered)
    } else if (
        studentDatabase.findOne(Student::firstName eq firstName) != null &&
        studentDatabase.findOne(Student::middleName eq middleName) != null &&
        studentDatabase.findOne(Student::lastName eq lastName) != null
    ) return Either.Left(StudentRegistrationError.StudentNameAlreadyExists)
    else if (studentDatabase.findOne(Student::studentCode eq studentCode) != null) {
        return Either.Left(StudentRegistrationError.StudentCodeAlreadyExists)
    }
    return Either.Right(
        verification(
            discordId = discordId,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            studentCode = studentCode,
            preferredName = preferredName
        )
    )
}

enum class StudentRegistrationError {
    AccountRegistered,
    StudentNameAlreadyExists,
    StudentCodeAlreadyExists
}

suspend fun registerCourse(
    simpleName: String,
    courseType: CourseType?,
    channel: Snowflake?,
): Either<CourseRegistrationError, Course> {
    if (courseDatabase.findOne(Course::simpleName eq simpleName) != null) {
        return Either.Left(CourseRegistrationError.CourseRegistered)
    } else if (courseDatabase.findOne(Course::channel eq channel) != null) {
        return Either.Left(CourseRegistrationError.ChannelAlreadyUsed)
    }
    val course = Course(
        simpleName = simpleName,
        courseType = courseType,
        channel = channel
    )
    courseDatabase.insertOne(course)
    return Either.Right(course)
}

enum class CourseRegistrationError {
    CourseRegistered,
    ChannelAlreadyUsed
}

suspend fun registerAssignment(
    name: String,
    dueDate: LocalDate?,
    weight: AssignmentWeight?,
    description: String?,
    courseId: Base64Id,
    teacher: Teacher? = null
): Either<AssignmentRegistrationError, Assignment> {
    val course = courseDatabase.findOneById(courseId)!!
    val assignment = Assignment(
        name = name,
        dueDate = dueDate,
        weight = weight,
        description = description,
        course = course,
        teacher = teacher?.teacherId
    )
    assignmentDatabase.insertOne(assignment)
    course.assignments += assignment.assignmentId
    courseDatabase.updateOneById(course.courseId, course)
    return Either.Right(assignment)
}

enum class AssignmentRegistrationError {


}
