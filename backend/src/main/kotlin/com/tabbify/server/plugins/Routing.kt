package com.tabbify.server.plugins

import com.tabbify.server.config.AppConfig
import com.tabbify.server.routing.authRouting
import com.tabbify.server.routing.recordingsRouting
import com.tabbify.server.routing.sessionsRouting
import com.tabbify.server.routing.songsRouting
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        route("/api") {
            authRouting(config.jwt)

            authenticate("auth-jwt") {
                songsRouting()
                recordingsRouting(config.storage)
                sessionsRouting()
            }
        }
    }
}
