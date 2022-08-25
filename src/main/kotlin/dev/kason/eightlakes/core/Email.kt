package dev.kason.eightlakes.core

import dev.kason.eightlakes.config
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

fun createMailerInstance() {
    val emailConfig = config.getConfig("bot.email")!!
    _emailUsername = emailConfig.getString("username")
    MailerManager.defaultMailer = mailerBuilder(
        host = runCatching { config.getString("host") }.getOrDefault("localhost"),
        port = runCatching { config.getInt("port") }.getOrDefault(25),
        username = emailUsername,
        password = emailConfig.getString("email.password")
    ) {
        val propertyConfig = emailConfig.getConfig("props")
        val keys = propertyConfig.root().keys
        for (key in keys) {
            properties["mail.$key"] = propertyConfig.getString(key)
        }
    }
}

suspend fun sendEmailTo(emailAddress: String, block: suspend EmailPopulatingBuilder.() -> Unit) {
    emailBuilder {
        from(emailAddress)
        to(emailAddress)
        block()
    }.send(MailerManager.defaultMailer)
}