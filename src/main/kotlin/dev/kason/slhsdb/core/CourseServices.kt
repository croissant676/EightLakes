package dev.kason.slhsdb.core

suspend fun findOtherStudents(student: Student, periodNumber: Period): List<Student> {
    val course = student.courses[periodNumber]?.course() ?: throw IllegalStateException()
    return course.students.map { it.student()!! }.filter { it.studentId != student.studentId }
}