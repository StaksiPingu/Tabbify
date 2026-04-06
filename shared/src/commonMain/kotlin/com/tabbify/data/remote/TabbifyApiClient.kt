package com.tabbify.data.remote

import com.tabbify.data.model.AuthRequest
import com.tabbify.data.model.AuthResponse
import com.tabbify.data.model.RegisterRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteSong(
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
data class CreateSongBody(
    val title: String,
    val bpm: Int,
    val timeSignature: String,
    val instrument: String
)

@Serializable
data class RemoteSession(
    val id: String,
    val instrument: String,
    val durationMs: Long,
    val score: Float,
    val notes: String,
    val recordedAt: Long
)

@Serializable
data class CreateSessionBody(
    val songId: String? = null,
    val instrument: String,
    val durationMs: Long,
    val score: Float,
    val criteriaScores: Map<String, Float> = emptyMap(),
    val notes: String = ""
)

class TabbifyApiClient(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenStorage.getToken() ?: return@loadTokens null
                    BearerTokens(accessToken = token, refreshToken = "")
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/api/auth/login") {
            setBody(AuthRequest(email, password))
        }.body<AuthResponse>()
    }

    suspend fun register(email: String, username: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/api/auth/register") {
            setBody(RegisterRequest(email, username, password))
        }.body<AuthResponse>()
    }

    // ── Songs ─────────────────────────────────────────────────────────────────

    suspend fun getSongs(): Result<List<RemoteSong>> = runCatching {
        client.get("$baseUrl/api/songs").body<List<RemoteSong>>()
    }

    suspend fun createSong(body: CreateSongBody): Result<RemoteSong> = runCatching {
        client.post("$baseUrl/api/songs") { setBody(body) }.body<RemoteSong>()
    }

    suspend fun updateSong(id: String, body: CreateSongBody): Result<Unit> = runCatching {
        client.put("$baseUrl/api/songs/$id") { setBody(body) }
    }

    suspend fun deleteSong(id: String): Result<Unit> = runCatching {
        client.delete("$baseUrl/api/songs/$id")
    }

    // ── Recordings ────────────────────────────────────────────────────────────

    suspend fun uploadRecording(
        trackId: String,
        fileBytes: ByteArray,
        durationMs: Long,
        mimeType: String = "audio/m4a"
    ): Result<String> = runCatching {
        val response = client.post("$baseUrl/api/recordings/upload") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("trackId", trackId)
                    append("durationMs", durationMs.toString())
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"recording.m4a\"")
                    })
                }
            ))
        }
        response.body<Map<String, String>>()["recordingId"] ?: ""
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    suspend fun getSessions(limit: Int = 20): Result<List<RemoteSession>> = runCatching {
        client.get("$baseUrl/api/sessions") {
            parameter("limit", limit)
        }.body<List<RemoteSession>>()
    }

    suspend fun createSession(body: CreateSessionBody): Result<RemoteSession> = runCatching {
        client.post("$baseUrl/api/sessions") { setBody(body) }.body<RemoteSession>()
    }

    fun close() = client.close()
}
