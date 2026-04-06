package com.tabbify.server.config

data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val storage: StorageConfig
)

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val allowedOrigins: List<String> = listOf("*")
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String = "org.postgresql.Driver",
    val maxPoolSize: Int = 10
)

data class JwtConfig(
    val secret: String,
    val issuer: String = "tabbify",
    val audience: String = "tabbify-users",
    val expiresInMs: Long = 7 * 24 * 60 * 60 * 1000L
)

data class StorageConfig(
    val path: String = "/data/recordings",
    val maxFileSizeMb: Long = 100
)
