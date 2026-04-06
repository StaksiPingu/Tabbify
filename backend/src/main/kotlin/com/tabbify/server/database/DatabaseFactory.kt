package com.tabbify.server.database

import com.tabbify.server.config.DatabaseConfig
import com.tabbify.server.model.Songs
import com.tabbify.server.model.Tracks
import com.tabbify.server.model.Users
import com.tabbify.server.model.PracticeSessions
import com.tabbify.server.model.ScoreConfigs
import com.tabbify.server.model.Recordings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            driverClassName = config.driver
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(hikariConfig))

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Songs,
                Tracks,
                Recordings,
                PracticeSessions,
                ScoreConfigs
            )
        }
    }
}
