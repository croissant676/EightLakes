package dev.kason.eightlakes.students

import dev.kason.eightlakes.utils.ConfigAware
import io.ktor.util.*
import kotlinx.datetime.Clock
import net.axay.simplekotlinmail.delivery.*
import org.kodein.di.DI
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

class VerificationService(override val di: DI) : ConfigAware(di) {
    companion object {
        @Suppress("LocalVariableName")
        val ExpirationDuration = 1.hours
    }

    private var createdMailerAlready: Boolean = false

    private suspend fun createMailerIfNotCreated() {
        if (createdMailerAlready) return
        createdMailerAlready = true
        MailerManager.defaultMailer = mailerBuilder(

        )
    }

    suspend fun openVerification(
        student: Student
    ): StudentVerification {
        createMailerIfNotCreated()
        val email = student.email
        val verification = StudentVerification.new {
            this.student = student
            this.token = Random.nextBytes(24).encodeBase64()
            this.email = email
            this.expirationDate = Clock.System.now() + ExpirationDuration
        }

        return verification
    }
}