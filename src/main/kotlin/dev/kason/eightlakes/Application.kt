package dev.kason.eightlakes

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.kason.eightlakes.core.models.*
import dev.kason.eightlakes.discord.addMessageListener
import dev.kason.eightlakes.discord.checkCounting
import dev.kason.eightlakes.discord.enableProfileCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.kordLogger
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.SecureRandom
import kotlin.random.asKotlinRandom
import kotlin.reflect.jvm.javaMethod

private var _kord: Kord? = null
val kord: Kord get() = _kord!!

val config: Config = ConfigFactory.defaultApplication()

val guildId = Snowflake(config.getString("bot.guild"))

private var _guild: Guild? = null
val guild: Guild get() = _guild!!

val random = SecureRandom().asKotlinRandom()

private var _database: Database? = null
val database: Database get() = _database!!

val applicationClass: Class<*> = ::main.javaMethod!!.declaringClass

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    _kord = Kord(config.getString("bot.token")) {
        kordLogger.info { "Created discord bot with application id: ${this.applicationId}" }
        stackTraceRecovery = true
    }
    _guild = kord.getGuild(guildId)
    _database = Database.connect(
        url = "jdbc:postgresql:eightlakes",
        user = config.getString("bot.data.user"),
        password = config.getString("bot.data.password"),
        databaseConfig = DatabaseConfig {
            sqlLogger = Slf4jSqlDebugLogger
        }
    )
    database.configure()
    discordModule()
    kord.login {
        intents = Intents.all
        presence { watching("students suffer") }
    }
}

suspend fun Database.configure() = newSuspendedTransaction {
    SchemaUtils.createMissingTablesAndColumns(
        Students,
        Teachers,
        Courses,
        StudentClasses,
        StudentVerifications
    )
    Students.createStatement()
}

suspend fun discordModule() {
    addMessageListener()
    checkCounting()
    enableProfileCommand()
}
