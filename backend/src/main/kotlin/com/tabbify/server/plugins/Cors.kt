package com.tabbify.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors(allowedOrigins: List<String>) {
    install(CORS) {
        allowedOrigins.forEach { origin ->
            if (origin == "*") anyHost()
            else allowHost(origin, schemes = listOf("https", "http"))
        }
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        exposeHeader(HttpHeaders.Authorization)
    }
}
