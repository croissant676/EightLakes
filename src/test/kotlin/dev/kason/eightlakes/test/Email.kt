package dev.kason.eightlakes.test

import kotlinx.html.*
import kotlinx.html.stream.appendHTML

fun main() {
    val block: TagConsumer<StringBuilder>.() -> StringBuilder = {
        head {
            meta {
                charset = "utf-8"
            }
            meta {
                name = "viewport"
                content = "width=device-width, initial-scale=1"
            }
            link("https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css", rel = "stylesheet")
            script(src = "https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js") {}
            script(src = "https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js") {}
        }
        body {
            div(classes = "text-center") {
                p {
                    +"""
                        # Hi, Test

                        # Thanks for signing up to Eight Lakes! To finish your signup, 
                        # we just need to confirm your email. Your verification code is: 
                    """.trimMargin("#")
                }
                h2 {
                    strong {
                        +"<Verification>"
                    }
                }
                p {
                    +"""
                        # This verification code expires in 1 hour. Again, thanks for 
                        # using our service!
                    """.trimMargin("#")
                }
            }
            div {
                p { +"Created for the Eight Lakes project." }
                a("https://github.com/croissant676/EightLakes") {
                    +"Visit the Github"
                }
            }
        }
    }
    println(StringBuilder().appendHTML().block())
}