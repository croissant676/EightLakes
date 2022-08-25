package dev.kason.eightlakes

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.jetbrains.exposed.sql.Database
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

private var _kord: Kord? = null
val kord: Kord get() = _kord!!

val config: Config = ConfigFactory.defaultApplication()

val guildId = Snowflake(config.getString(""))

private var _guild: Guild? = null
val guild: Guild get() = _guild!!

val random = SecureRandom().asKotlinRandom()

private var _database: Database? = null
val database: Database get() = _database!!

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    _kord = Kord(System.getProperty("bot.token")) {
    }
    kord.login {
        intents = Intents.all
        presence { watching("students suffer") }
    }
    _guild = kord.getGuild(guildId)
    _database = Database.connect(
        url = "jdbc:postgresql:eightlakes",
        user = System.getProperty("data.user"),
        password = System.getProperty("data.password")
    )
}

