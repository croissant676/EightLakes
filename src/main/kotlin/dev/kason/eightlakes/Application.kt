package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.students.Student
import org.kodein.di.*
import uy.klutter.config.typesafe.loadApplicationConfig

class EightLakesApp(override val di: DI) : DIAware {
    companion object {

    }

    val config: Config by di.instance()

}

suspend fun main(args: Array<String>) {
    val di = DI {
        fullDescriptionOnError = true
        fullContainerTreeOnError = true
        bindSingleton { loadApplicationConfig() }
        bindSingleton { EightLakesApp(di) }
        importAll(
            Student.createModule()
        )
    }
}