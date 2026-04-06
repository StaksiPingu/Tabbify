package com.tabbify.server.routing

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.tabbify.server.config.JwtConfig
import com.tabbify.server.model.Users
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date
import java.util.UUID

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val username: String, val email: String)

fun Route.authRouting(jwtConfig: JwtConfig) {
    route("/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.email.isBlank() || req.password.length < 8) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email or password too short"))
                return@post
            }

            val userId = transaction {
                val existing = Users.select { Users.email eq req.email }.firstOrNull()
                if (existing != null) return@transaction null

                val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
                val id = UUID.randomUUID()
                Users.insert {
                    it[Users.id] = org.jetbrains.exposed.dao.id.EntityID(id, Users)
                    it[email] = req.email
                    it[username] = req.username
                    it[passwordHash] = hash
                    it[createdAt] = Clock.System.now()
                }
                id.toString()
            }

            if (userId == null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                return@post
            }

            val token = generateToken(userId, jwtConfig)
            call.respond(HttpStatusCode.Created, AuthResponse(token, userId, req.username, req.email))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()

            val user = transaction {
                Users.select { Users.email eq req.email }.firstOrNull()
            }

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val passwordMatch = BCrypt.verifyer()
                .verify(req.password.toCharArray(), user[Users.passwordHash])
                .verified

            if (!passwordMatch) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val userId = user[Users.id].value.toString()
            val token = generateToken(userId, jwtConfig)
            call.respond(AuthResponse(token, userId, user[Users.username], user[Users.email]))
        }
    }
}

private fun generateToken(userId: String, config: JwtConfig): String {
    return JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + config.expiresInMs))
        .sign(Algorithm.HMAC256(config.secret))
}
