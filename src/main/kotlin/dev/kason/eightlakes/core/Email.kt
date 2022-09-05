package dev.kason.eightlakes.core

import dev.kason.eightlakes.*
import net.axay.simplekotlinmail.delivery.*
import uy.klutter.config.typesafe.value

private var _emailUsername: String? = null
val emailUsername: String
    get() {
        if (_emailUsername == null) registerMailer()
        return _emailUsername!!
    }

fun registerMailer() {
    _emailUsername = config.value("email.username").asString()
    val host = config.value("email.host").asString("localhost")
    val port = config.value("email.port").asInt(25)
    val password = config.value("email.password").asStringOrNull()
    MailerManager.defaultMailer = mailerBuilder(
        host = host,
        port = port,
        username = emailUsername,
        password = password
    ) {
        val propsConfig = config.value("email.props").asObjectOrNull()?.toConfig()
            ?: return@mailerBuilder
        val propsAsMap = propsConfig.entrySet().associate { it.key to propsConfig.getString(it.key) }
        for ((string, configuredValue) in propsAsMap) {
            properties["mail.$string"] = configuredValue
        }
        logger.debug { "Inserted properties $propsAsMap into mailer.properties" }
    }
}