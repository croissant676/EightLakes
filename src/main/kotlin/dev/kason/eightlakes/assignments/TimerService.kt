package dev.kason.eightlakes.assignments

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.kodein.di.*
import java.util.*

class TimerService(override val di: DI) : DIAware {
    private var run: Boolean = true

    private val timerFlow = flow {
        while (run) {
            val currentLongCount = System.currentTimeMillis()
            val millisUntilNextSecond = 1000 - (currentLongCount % 1000)
            delay(millisUntilNextSecond)
            emit(Unit)
        }
    }

    private val priorityQueue = PriorityQueue<TimerTask>()

    internal class TimerTask(val time: Long, val action: suspend () -> Unit) : Comparable<TimerTask> {
        constructor(time: Instant, action: suspend () -> Unit) : this(time.toEpochMilliseconds(), action)
        constructor(
            time: LocalDateTime,
            action: suspend () -> Unit
        ) : this(time.toInstant(TimeZone.currentSystemDefault()), action)

        override fun compareTo(other: TimerTask): Int {
            return time.compareTo(other.time)
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        while (run) {
            timerFlow.collect {
                val currentTime = System.currentTimeMillis()
                while (priorityQueue.isNotEmpty() && priorityQueue.peek().time <= currentTime) {
                    val task = priorityQueue.poll()
                    task.action()
                }
            }
        }
    }

    fun stop() {
        run = false
    }

    private fun checkIfTimeHasPassed(instant: Instant): Boolean {
        return instant.toEpochMilliseconds() <= System.currentTimeMillis()
    }

    fun schedule(time: LocalDateTime, action: suspend () -> Unit) {
        require(!checkIfTimeHasPassed(time.toInstant(TimeZone.currentSystemDefault()))) { "$time has already passed." }
        priorityQueue.add(TimerTask(time, action))
    }

    fun schedule(time: Instant, action: suspend () -> Unit) {
        require(!checkIfTimeHasPassed(time)) { "$time has already passed." }
        priorityQueue.add(TimerTask(time, action))
    }
}