package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.courses.Courses
import dev.kason.eightlakes.students.*
import dev.kason.eightlakes.utils.ConfigAware
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.gateway.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

        fun connectToDatabase(config: Config): Database {
            return Database.connect(
                url = config.getString("data.url"),
                user = config.getString("data.user"),
                password = config.getString("data.password"),
                databaseConfig = DatabaseConfig {
                    sqlLogger = Slf4jSqlDebugLogger
                }
            )
        }

    }

    private val kord: Kord by di.instance()
    private val studentService: StudentService by di.instance()

    @OptIn(PrivilegedIntent::class)
    suspend fun start() {
        updateDatabase()
        testOnStart()
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

    private suspend fun testOnStart() {
        val student = studentService.signup(
            "Kason",
            "Kaixuan",
            "Gu",
            null,
            "G1003409",
            "08/30/2006",
            kord.getUser(Snowflake(764180149251080192))!!
        )
        print(student)
    }

    override val coroutineContext: CoroutineContext get() = kord.coroutineContext
}

suspend fun main() {
    // Initialized before di is created because we need to access suspend (Continuation)
    val config = loadApplicationConfig()
    val kord = EightLakesApp.createKord(config)
    val database = EightLakesApp.connectToDatabase(config) // Must load before tables are loaded in.
    val di = DI {
        fullDescriptionOnError = true
        fullContainerTreeOnError = true
        bindSingleton { config }
        bindSingleton { kord }
        bindSingleton { EightLakesApp(di) }
        bindSingleton { database }
        importAll(
            Student.createModule()
        )
    }
    val app: EightLakesApp = di.direct.instance()
    app.start()
}