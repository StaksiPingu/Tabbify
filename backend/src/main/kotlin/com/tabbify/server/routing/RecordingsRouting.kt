package com.tabbify.server.routing

import com.tabbify.server.config.StorageConfig
import com.tabbify.server.model.Recordings
import com.tabbify.server.model.Tracks
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID

fun Route.recordingsRouting(storageConfig: StorageConfig) {
    val storageDir = File(storageConfig.path).also { it.mkdirs() }
    val maxFileSizeBytes = storageConfig.maxFileSizeMb * 1024 * 1024

    route("/recordings") {
        post("/upload") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val multipart = call.receiveMultipart()

            var trackId: String? = null
            var fileBytes: ByteArray? = null
            var mimeType = "audio/m4a"
            var durationMs = 0L

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> when (part.name) {
                        "trackId" -> trackId = part.value
                        "durationMs" -> durationMs = part.value.toLongOrNull() ?: 0L
                    }
                    is PartData.FileItem -> {
                        mimeType = part.contentType?.toString() ?: "audio/m4a"
                        fileBytes = part.streamProvider().readBytes()
                    }
                    else -> {}
                }
                part.dispose()
            }

            val tid = trackId ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "trackId required"))
                return@post
            }
            val bytes = fileBytes ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "file required"))
                return@post
            }

            if (bytes.size > maxFileSizeBytes) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "File too large"))
                return@post
            }

            val recordingId = UUID.randomUUID()
            val fileKey = "recordings/$userId/$tid/$recordingId.m4a"
            val targetFile = File(storageDir, fileKey).also { it.parentFile.mkdirs() }
            targetFile.writeBytes(bytes)

            transaction {
                Recordings.insert {
                    it[Recordings.id] = EntityID(recordingId, Recordings)
                    it[Recordings.trackId] = EntityID(UUID.fromString(tid), Tracks)
                    it[Recordings.userId] = EntityID(UUID.fromString(userId), com.tabbify.server.model.Users)
                    it[Recordings.fileKey] = fileKey
                    it[Recordings.fileSizeBytes] = bytes.size.toLong()
                    it[Recordings.durationMs] = durationMs
                    it[Recordings.mimeType] = mimeType
                    it[Recordings.createdAt] = Clock.System.now()
                }
            }

            call.respond(HttpStatusCode.Created, mapOf(
                "recordingId" to recordingId.toString(),
                "fileKey" to fileKey
            ))
        }

        get("/{recordingId}/file") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val recordingId = call.parameters["recordingId"] ?: throw IllegalArgumentException("Recording ID required")

            val recording = transaction {
                Recordings.select { Recordings.id eq UUID.fromString(recordingId) }.firstOrNull()
            } ?: throw NoSuchElementException("Recording not found")

            if (recording[Recordings.userId].value.toString() != userId) {
                throw SecurityException("Access denied")
            }

            val file = File(storageDir, recording[Recordings.fileKey])
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respondFile(file)
        }
    }
}
