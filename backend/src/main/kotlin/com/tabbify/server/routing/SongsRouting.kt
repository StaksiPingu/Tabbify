package com.tabbify.server.routing

import com.tabbify.server.model.Songs
import com.tabbify.server.model.Tracks
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val bpm: Int,
    val timeSignature: String,
    val instrument: String,
    val trackCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateSongRequest(
    val title: String,
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val instrument: String = "GUITAR"
)

fun Route.songsRouting() {
    route("/songs") {
        get {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()

            val songs = transaction {
                Songs
                    .select { Songs.userId eq UUID.fromString(userId) }
                    .orderBy(Songs.updatedAt, SortOrder.DESC)
                    .map { row ->
                        val songId = row[Songs.id].value
                        val trackCount = Tracks.select { Tracks.songId eq songId }.count().toInt()
                        SongDto(
                            id = songId.toString(),
                            title = row[Songs.title],
                            bpm = row[Songs.bpm],
                            timeSignature = row[Songs.timeSignature],
                            instrument = row[Songs.instrument],
                            trackCount = trackCount,
                            createdAt = row[Songs.createdAt].epochSeconds,
                            updatedAt = row[Songs.updatedAt].epochSeconds
                        )
                    }
            }

            call.respond(songs)
        }

        post {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val req = call.receive<CreateSongRequest>()

            if (req.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Title required"))
                return@post
            }

            val songId = UUID.randomUUID()
            val now = Clock.System.now()

            transaction {
                Songs.insert {
                    it[Songs.id] = EntityID(songId, Songs)
                    it[Songs.userId] = EntityID(UUID.fromString(userId), com.tabbify.server.model.Users)
                    it[title] = req.title
                    it[bpm] = req.bpm
                    it[timeSignature] = req.timeSignature
                    it[instrument] = req.instrument
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            call.respond(HttpStatusCode.Created, SongDto(
                id = songId.toString(),
                title = req.title,
                bpm = req.bpm,
                timeSignature = req.timeSignature,
                instrument = req.instrument,
                createdAt = now.epochSeconds,
                updatedAt = now.epochSeconds
            ))
        }

        put("/{id}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val songId = call.parameters["id"] ?: throw IllegalArgumentException("Song ID required")
            val req = call.receive<CreateSongRequest>()

            val updated = transaction {
                Songs.update({
                    (Songs.id eq UUID.fromString(songId)) and
                    (Songs.userId eq UUID.fromString(userId))
                }) {
                    it[title] = req.title
                    it[bpm] = req.bpm
                    it[timeSignature] = req.timeSignature
                    it[instrument] = req.instrument
                    it[updatedAt] = Clock.System.now()
                }
            }

            if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Song not found"))
            else call.respond(HttpStatusCode.OK, mapOf("updated" to true))
        }

        delete("/{id}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val songId = call.parameters["id"] ?: throw IllegalArgumentException("Song ID required")

            val deleted = transaction {
                Songs.deleteWhere {
                    (Songs.id eq UUID.fromString(songId)) and
                    (Songs.userId eq UUID.fromString(userId))
                }
            }

            if (deleted == 0) call.respond(HttpStatusCode.NotFound)
            else call.respond(HttpStatusCode.NoContent)
        }
    }
}
