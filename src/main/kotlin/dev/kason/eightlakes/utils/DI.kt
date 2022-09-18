package dev.kason.eightlakes.utils

import org.kodein.di.DI

interface ModuleProducer {

    fun createModule(): DI.Module

}