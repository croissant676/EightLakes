package dev.kason.eightlakes.students

import dev.kord.core.Kord
import org.kodein.di.*

class StudentController(override val di: DI) : DIAware {
    private val kord: Kord by di.instance()
    private val studentService: StudentService by di.instance()
    private val verificationService: VerificationService by di.instance()
}
