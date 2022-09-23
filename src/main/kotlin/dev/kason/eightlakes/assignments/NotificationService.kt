package dev.kason.eightlakes.assignments

import org.kodein.di.*

class NotificationService(override val di: DI) : DIAware {

    val timerService: TimerService by di.instance()

    fun start() {

    }

}