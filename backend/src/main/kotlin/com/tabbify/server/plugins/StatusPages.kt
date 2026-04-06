package com.tabbify.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiError(val error: String, val message: String)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("BAD_REQUEST", cause.message ?: "Invalid input"))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", cause.message ?: "Resource not found"))
        }
        exception<SecurityException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", cause.message ?: "Access denied"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("INTERNAL_ERROR", "An unexpected error occurred"))
        }
    }
}
