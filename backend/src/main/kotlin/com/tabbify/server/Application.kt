package com.tabbify.server

import com.tabbify.server.config.AppConfig
import com.tabbify.server.database.DatabaseFactory
import com.tabbify.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

fun main() {
    val config = ConfigLoaderBuilder.default()
        .addResourceSource("/application.conf")
        .build()
        .loadConfigOrThrow<AppConfig>()

    DatabaseFactory.init(config.database)

    embeddedServer(
        factory = Netty,
        port = config.server.port,
        host = config.server.host,
        module = { tabbifyModule(config) }
    ).start(wait = true)
}

fun Application.tabbifyModule(config: AppConfig) {
    configureSecurity(config.jwt)
    configureSerialization()
    configureCors(config.server.allowedOrigins)
    configureStatusPages()
    configureRouting(config)
    configureCallLogging()
}
