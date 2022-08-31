package dev.kason.eightlakes.core

import com.typesafe.config.ConfigObject
import dev.kason.eightlakes.config
import io.github.config4k.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.axay.simplekotlinmail.delivery.MailerManager
import net.axay.simplekotlinmail.delivery.mailerBuilder
import net.axay.simplekotlinmail.delivery.send
import net.axay.simplekotlinmail.email.emailBuilder
import org.simplejavamail.api.email.EmailPopulatingBuilder

private var _emailUsername: String? = null
val emailUsername: String
    get() {
        if (_emailUsername == null) createMailerInstance()
        return _emailUsername!!
    }

private val emailLogger = KotlinLogging.logger { }

fun createMailerInstance() {
    val emailConfig = config.getConfig("bot.email")!!
    _emailUsername = emailConfig.getString("username")
    val host: String? by emailConfig
    val port: Int? by emailConfig
    emailLogger.debug { "Mailer = {$host:$port}, username = $emailUsername" }
    MailerManager.defaultMailer = mailerBuilder(
        host = host ?: "localhost",
        port = port ?: 25,
        username = emailUsername,
        password = emailConfig.getString("password")
    ) {
        val propertyConfig = emailConfig.getConfig("props").root()
        // simple bfs through the config tree
        val objectQueue = ArrayDeque<ConfigObject>()
        objectQueue += propertyConfig
        while (objectQueue.isNotEmpty()) {
            val configObject = objectQueue.removeFirst()
            configObject
        }
    }
}

suspend fun sendEmailTo(emailAddress: String, block: suspend EmailPopulatingBuilder.() -> Unit) =
    withContext(Dispatchers.IO) {
        emailBuilder {
            from(emailAddress)
            to(emailAddress)
            block()
        }.send(MailerManager.defaultMailer)
    }