package com.tabbify.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val instrument: InstrumentType = InstrumentType.GUITAR,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncedAt: Long? = null,
    val userId: String? = null
)

@Serializable
data class Track(
    val id: String,
    val songId: String,
    val name: String,
    val instrument: InstrumentType,
    val audioFilePath: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val durationMs: Long = 0L,
    val createdAt: Long = 0L,
    val analysis: TrackAnalysis? = null
)

@Serializable
data class TrackAnalysis(
    val averagePitch: Float = 0f,
    val pitchAccuracy: Float = 0f,
    val rhythmConsistency: Float = 0f,
    val dynamicsRange: Float = 0f,
    val detectedBpm: Float? = null,
    val waveformData: List<Float> = emptyList(),
    val analyzedAt: Long = 0L
)
