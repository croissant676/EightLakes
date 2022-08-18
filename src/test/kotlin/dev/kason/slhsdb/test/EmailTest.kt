package dev.kason.slhsdb.test

import dev.kason.slhsdb.core.registerMailer
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.StringSpec

class EmailTest : StringSpec({
    beforeAny {
        System.setProperty("email.username", "kasongukkg@gmail.com")
        System.setProperty("email.password", "tfmgfrikpfysddau")
    }
    "Send email to aidan" {
        runBlocking {
            registerMailer()
        }
    }
})