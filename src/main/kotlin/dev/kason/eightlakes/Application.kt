package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.courses.Courses
import dev.kason.eightlakes.discord.DiscordService
import dev.kason.eightlakes.students.*
import dev.kason.eightlakes.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.gateway.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

        private fun connectToDatabase(config: Config): Database {
            return Database.connect(
                url = config.getString("data.url"),
                user = config.getString("data.user"),
                password = config.getString("data.password"),
                databaseConfig = DatabaseConfig {
                    sqlLogger = Slf4jSqlDebugLogger
                }
            )
        }

        private suspend fun getGuild(kord: Kord, config: Config): Guild {
            return kord.getGuild(Snowflake(config.getLong("bot.guild")))
                ?: error("Guild not found")
        }

        override suspend fun createModule(): DI.Module {
            val config = loadApplicationConfig()
            val kord = createKord(config)
            val database = connectToDatabase(config)
            val application = kord.getApplicationInfo()
            val guild = getGuild(kord, config)
            return DI.Module("core_module") {
                bindSingleton { config }
                bindSingleton { kord }
                bindSingleton { database }
                bindSingleton { application }
                bindSingleton { guild }
            }
        }

        suspend fun noDatabase(): DI.Module {
            val config = loadApplicationConfig()
            val kord = createKord(config)
            val application = kord.getApplicationInfo()
            val guild = getGuild(kord, config)
            return DI.Module("core_module_no_db") {
                bindSingleton { config }
                bindSingleton { kord }
                bindSingleton { application }
                bindSingleton { guild }
            }
        }
    }

    private val kord: Kord by di.instance()
    private val studentService: StudentService by di.instance()

    @OptIn(PrivilegedIntent::class)
    suspend fun start() {
        updateDatabase()
//        testOnStart()
        kord.login {
            intents += Intents.all
            presence {
                watching("students suffer")
            }
        }
    }

    private suspend fun updateDatabase() {
        newSuspendedTransaction(Dispatchers.IO) {
            SchemaUtils.createMissingTablesAndColumns(
                Students,
                StudentVerifications,
                Courses
            )
        }
    }

    override val coroutineContext: CoroutineContext get() = kord.coroutineContext
}

suspend fun main() {
    val modules = setOf(
        EightLakesApp.createModule(),
        Student.Loader.createModule(),
        DiscordService.createModule()
    )
    val di = DI {
        fullDescriptionOnError = true
        fullContainerTreeOnError = true
        importAll(modules)
    }
    val app by di.newInstance { EightLakesApp(di) }
    app.start()
}