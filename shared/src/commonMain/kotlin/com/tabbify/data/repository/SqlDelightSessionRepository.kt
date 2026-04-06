package com.tabbify.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.PracticeSession
import com.tabbify.db.TabbifyDatabase
import com.tabbify.domain.scoring.DefaultScoreConfigs
import com.tabbify.domain.scoring.ScoreConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SqlDelightSessionRepository(
    private val db: TabbifyDatabase
) : SessionRepository {

    private val queries = db.songsQueries
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>> {
        return queries.getAllSessions(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toSession() } }
    }

    override suspend fun getSessionById(id: String): PracticeSession? = withContext(Dispatchers.Default) {
        null // TODO: add getSessonById query to .sq file
    }

    override suspend fun saveSession(session: PracticeSession): PracticeSession = withContext(Dispatchers.Default) {
        val id = session.id.ifBlank { uuid4().toString() }
        val now = session.recordedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        queries.insertSession(
            id = id,
            song_id = session.songId,
            track_id = session.trackId,
            instrument = session.instrument.name,
            duration_ms = session.durationMs,
            score = session.score.toDouble(),
            criteria_scores = json.encodeToString(session.criteriaScores),
            notes = session.notes,
            recorded_at = now,
            synced_at = null
        )
        session.copy(id = id, recordedAt = now)
    }

    override suspend fun deleteSession(id: String): Unit = withContext(Dispatchers.Default) {
        queries.deleteSession(id)
    }

    override suspend fun getTotalPracticeTimeMs(): Long = withContext(Dispatchers.Default) {
        queries.getTotalPracticeMs().executeAsOne()
    }

    override suspend fun getCurrentStreak(): Int {
        // Simplified: count distinct days with sessions in last N days
        return 0 // TODO: implement day-streak calculation
    }

    override suspend fun getScoreConfig(instrument: InstrumentType): ScoreConfig? = withContext(Dispatchers.Default) {
        val row = queries.getScoreConfig(instrument.name).executeAsOneOrNull() ?: return@withContext null
        runCatching { json.decodeFromString<ScoreConfig>(row) }.getOrNull()
    }

    override suspend fun saveScoreConfig(config: ScoreConfig): Unit = withContext(Dispatchers.Default) {
        queries.upsertScoreConfig(
            instrument = config.instrumentType.name,
            config_json = json.encodeToString(config)
        )
    }
}

private fun com.tabbify.db.Practice_sessions.toSession(): PracticeSession {
    val json = Json { ignoreUnknownKeys = true }
    val criteriaMap = runCatching {
        json.decodeFromString<Map<String, Float>>(criteria_scores)
    }.getOrDefault(emptyMap())

    return PracticeSession(
        id = id,
        songId = song_id,
        trackId = track_id,
        instrument = runCatching { InstrumentType.valueOf(instrument) }.getOrDefault(InstrumentType.GUITAR),
        durationMs = duration_ms,
        score = score.toFloat(),
        criteriaScores = criteriaMap,
        notes = notes,
        recordedAt = recorded_at,
        syncedAt = synced_at
    )
}
