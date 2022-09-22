package dev.kason.eightlakes.data

import dev.kason.eightlakes.utils.*
import org.kodein.di.DI

class EightLakesData(override val di: DI) : ConfigAware(di) {
    companion object : ModuleProducer {
        override suspend fun createModule(): DI.Module {
            return DI.Module(name = "data_module") {

            }
        }
    }


}