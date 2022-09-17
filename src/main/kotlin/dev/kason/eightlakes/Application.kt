package dev.kason.eightlakes

import dev.kason.eightlakes.core.*
import dev.kason.eightlakes.core.data.*
import dev.kason.eightlakes.discord.*
import dev.kason.eightlakes.discord.coursereg.Registrations
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.*
import dev.kord.gateway.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import uy.klutter.config.typesafe.*
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

private var _kord: Kord? = null
val kord: Kord get() = _kord!!

val config = loadConfig(ApplicationConfig())

val guildId = Snowflake(config.getLong("bot.guild"))

private var _guild: Guild? = null
val guild: Guild get() = _guild!!

val logger = KotlinLogging.logger { }

val random = SecureRandom().asKotlinRandom()


private var _database: Database? = null
val database: Database get() = _database!!

private var _application: Application? = null
val application: Application get() = _application!!

val appId = Snowflake(config.getLong("bot.appId"))

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    logger.info { "Starting application. May take a while to startup (around 10 - 20 seconds)" }
    _kord = Kord(config.getString("bot.token")) {
        stackTraceRecovery = true
    }
    _application = kord.getApplicationInfo()
    _guild = kord.getGuild(guildId)
    _database = Database.connect(
        url = config.getString("data.url"),
        user = config.getString("data.user"),
        password = config.getString("data.password"),
        databaseConfig = DatabaseConfig {
            sqlLogger = Slf4jSqlDebugLogger
        }
    )
    updateTablesAndColumns()
    registerCommandListener()
    registerMailer()
    registerAllCommands()
    registerMessageListener()
    registerCountingBotListener()
    kord.login {
        intents {
            +Intents.all
        }
        presence {
            watching("students suffer")
        }
    }
}

private suspend fun updateTablesAndColumns() = suspendTransaction {
    SchemaUtils.createMissingTablesAndColumns(
        Assignments,
        StudentAssignments,
        CourseClasses,
        StudentClasses,
        Courses,
        Students,
        StudentVerifications,
        Teachers,
        Registrations
    )
}