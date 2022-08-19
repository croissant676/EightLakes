package dev.kason.slhsdb

import dev.kason.slhsdb.core.registerMailer
import dev.kason.slhsdb.disc.addRegistrationCommand
import dev.kason.slhsdb.disc.addVerificationCommand
import dev.kason.slhsdb.disc.addVerifiedRole
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val database = KMongo.createClient().coroutine.getDatabase("slhs")

lateinit var kord: Kord
    private set

val guildId = Snowflake(1009290043615084666)

suspend fun main() {
    kord = Kord(System.getProperty("kord.token"))
    registerCommandSorter()
    registerMailer()
    addRegistrationCommand()
    addVerificationCommand()

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intents.all
        presence {
            watching("students struggle")
        }
    }
}