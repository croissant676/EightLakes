package dev.kason.eightlakes.utils

import com.typesafe.config.Config
import org.kodein.di.*

interface ModuleProducer {

    suspend fun createModule(config: Config): DI.Module

}

open class ConfigAware(
    override val di: DI
) : DIAware {

    // lazy init because referencing di may result in issues:
    // see: https://stackoverflow.com/questions/50222139/kotlin-calling-non-final-function-in-constructor-works
    val config: Config by lazy(LazyThreadSafetyMode.NONE) { di.direct.instance() }

}