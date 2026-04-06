package com.tabbify.server.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 100)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
}

object Songs : UUIDTable("songs") {
    val userId = reference("user_id", Users)
    val title = varchar("title", 255)
    val bpm = integer("bpm").default(120)
    val timeSignature = varchar("time_signature", 10).default("4/4")
    val instrument = varchar("instrument", 50).default("GUITAR")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object Tracks : UUIDTable("tracks") {
    val songId = reference("song_id", Songs)
    val name = varchar("name", 255)
    val instrument = varchar("instrument", 50)
    val audioFileKey = varchar("audio_file_key", 512)
    val volume = float("volume").default(1.0f)
    val isMuted = bool("is_muted").default(false)
    val durationMs = long("duration_ms").default(0L)
    val createdAt = timestamp("created_at")
}

object Recordings : UUIDTable("recordings") {
    val trackId = reference("track_id", Tracks)
    val userId = reference("user_id", Users)
    val fileKey = varchar("file_key", 512)
    val fileSizeBytes = long("file_size_bytes").default(0L)
    val durationMs = long("duration_ms").default(0L)
    val mimeType = varchar("mime_type", 100).default("audio/m4a")
    val pitchAccuracy = float("pitch_accuracy").nullable()
    val rhythmConsistency = float("rhythm_consistency").nullable()
    val dynamicsRange = float("dynamics_range").nullable()
    val detectedBpm = float("detected_bpm").nullable()
    val createdAt = timestamp("created_at")
}

object PracticeSessions : UUIDTable("practice_sessions") {
    val userId = reference("user_id", Users)
    val songId = reference("song_id", Songs).nullable()
    val instrument = varchar("instrument", 50)
    val durationMs = long("duration_ms").default(0L)
    val score = float("score").default(0f)
    val criteriaScoresJson = text("criteria_scores_json").default("{}")
    val notes = text("notes").default("")
    val recordedAt = timestamp("recorded_at")
}

object ScoreConfigs : Table("score_configs") {
    val userId = reference("user_id", Users)
    val instrument = varchar("instrument", 50)
    val configJson = text("config_json")
    override val primaryKey = PrimaryKey(userId, instrument)
}
