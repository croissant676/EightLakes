package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.students.Student
import dev.kason.eightlakes.utils.ConfigAware
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.gateway.*
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.*
import uy.klutter.config.typesafe.loadApplicationConfig
import kotlin.coroutines.CoroutineContext

class EightLakesApp(override val di: DI) : ConfigAware(di), CoroutineScope {
    companion object {
        suspend fun createKord(config: Config): Kord {
            val token = config.getString("bot.token")
            return Kord(token) {
                applicationId = Snowflake(config.getLong("bot.application-id"))
            }
        }
    }

    private val kord: Kord by di.instance()

    @OptIn(PrivilegedIntent::class)
    suspend fun start() {
        kord.login {
            intents += Intents.all
            presence {
                watching("students suffer")
            }
        }
    }

    override val coroutineContext: CoroutineContext get() = kord.coroutineContext
}

suspend fun main() {
    // Initialized before di is created because we need to access suspend (Continuation)
    val config = loadApplicationConfig()
    val kord = EightLakesApp.createKord(config)
    val di = DI {
        fullDescriptionOnError = true
        fullContainerTreeOnError = true
        bindSingleton { config }
        bindSingleton { kord }
        bindSingleton { EightLakesApp(di) }
        importAll(
            Student.createModule()
        )
    }
    val app: EightLakesApp = di.direct.instance()
    app.start()
}