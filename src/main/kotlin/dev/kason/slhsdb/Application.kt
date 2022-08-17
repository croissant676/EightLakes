package dev.kason.slhsdb

import dev.kason.slhsdb.students.userModule
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent

lateinit var kord: Kord
    private set

suspend fun main() {
    kord = Kord(System.getProperty("kord.token"))
    userModule()
    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}

suspend fun ping() {

}