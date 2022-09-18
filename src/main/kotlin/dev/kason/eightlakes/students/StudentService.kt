package dev.kason.eightlakes.students

import org.kodein.di.*

class StudentService(override val di: DI) : DIAware {
    suspend fun signup(
        firstName: String,
        middleName: String,
        lastName: String,

        ) {

    }
}