package dev.kason.eightlakes.students

import dev.kord.core.Kord
import org.kodein.di.*

class StudentController(override val di: DI) : DIAware {
    val kord: Kord by di.instance()
}
