package com.tabbify.server.routing

import com.tabbify.server.model.PracticeSessions
import com.tabbify.server.model.Songs
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class CreateSessionRequest(
    val songId: String? = null,
    val instrument: String,
    val durationMs: Long,
    val score: Float,
    val criteriaScores: Map<String, Float> = emptyMap(),
    val notes: String = ""
)

@Serializable
data class SessionDto(
    val id: String,
    val instrument: String,
    val durationMs: Long,
    val score: Float,
    val notes: String,
    val recordedAt: Long
)

fun Route.sessionsRouting() {
    route("/sessions") {
        get {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            val sessions = transaction {
                PracticeSessions
                    .select { PracticeSessions.userId eq UUID.fromString(userId) }
                    .orderBy(PracticeSessions.recordedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        SessionDto(
                            id = row[PracticeSessions.id].value.toString(),
                            instrument = row[PracticeSessions.instrument],
                            durationMs = row[PracticeSessions.durationMs],
                            score = row[PracticeSessions.score],
                            notes = row[PracticeSessions.notes],
                            recordedAt = row[PracticeSessions.recordedAt].epochSeconds
                        )
                    }
            }

            call.respond(sessions)
        }

        post {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val req = call.receive<CreateSessionRequest>()
            val sessionId = UUID.randomUUID()
            val now = Clock.System.now()

            transaction {
                PracticeSessions.insert {
                    it[PracticeSessions.id] = EntityID(sessionId, PracticeSessions)
                    it[PracticeSessions.userId] = EntityID(UUID.fromString(userId), com.tabbify.server.model.Users)
                    it[PracticeSessions.songId] = req.songId?.let { sid ->
                        EntityID(UUID.fromString(sid), Songs)
                    }
                    it[instrument] = req.instrument
                    it[durationMs] = req.durationMs
                    it[score] = req.score
                    it[criteriaScoresJson] = kotlinx.serialization.json.Json.encodeToString(
                        kotlinx.serialization.serializer<Map<String, Float>>(),
                        req.criteriaScores
                    )
                    it[notes] = req.notes
                    it[recordedAt] = now
                }
            }

            call.respond(HttpStatusCode.Created, SessionDto(
                id = sessionId.toString(),
                instrument = req.instrument,
                durationMs = req.durationMs,
                score = req.score,
                notes = req.notes,
                recordedAt = now.epochSeconds
            ))
        }
    }
}
