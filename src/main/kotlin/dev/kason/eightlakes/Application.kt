package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.discord.DiscordService
import dev.kason.eightlakes.students.Student
import dev.kason.eightlakes.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.gateway.*
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.kodein.di.*
import uy.klutter.config.typesafe.loadApplicationConfig
import kotlin.coroutines.CoroutineContext

class EightLakesApp(override val di: DI) : ConfigAware(di), CoroutineScope {
    companion object : ModuleProducer {

        private suspend fun createKord(config: Config): Kord {
            val token = config.getString("bot.token")
            return Kord(token) {
                applicationId = Snowflake(config.getLong("bot.application-id"))
            }
        }

        private suspend fun getGuild(kord: Kord, config: Config): Guild {
            return kord.getGuild(Snowflake(config.getLong("bot.guild")))
                ?: error("Guild not found")
        }

        override suspend fun createModule(config: Config): DI.Module {
            val kord = createKord(config)
            val application = kord.getApplicationInfo()
            val guild = getGuild(kord, config)
            return DI.Module("core_module") {
                bindSingleton { config }
                bindSingleton { kord }
                bindSingleton { application }
                bindSingleton { guild }
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

val eightLakesLogger = KotlinLogging.logger("EightLakes")

suspend fun main() {
    eightLakesLogger.info { "Started appication; may take up to 20 seconds to load everything." }
    val config = loadApplicationConfig()
    val modules = setOf(
        EightLakesApp.createModule(config),
        Student.Loader.createModule(config),
        DiscordService.createModule(config)
    )
    val di = DI {
        fullDescriptionOnError = true
        fullContainerTreeOnError = true
        importAll(modules)
    }
    val app by di.newInstance { EightLakesApp(di) }
    app.start()
}