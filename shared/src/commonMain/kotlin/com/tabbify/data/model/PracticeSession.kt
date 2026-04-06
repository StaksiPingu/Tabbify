package com.tabbify.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeSession(
    val id: String,
    val songId: String? = null,
    val trackId: String? = null,
    val instrument: InstrumentType,
    val durationMs: Long = 0L,
    val score: Float = 0f,
    val criteriaScores: Map<String, Float> = emptyMap(),
    val notes: String = "",
    val recordedAt: Long = 0L,
    val syncedAt: Long? = null
)
