package com.tabbify.data.repository

import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.PracticeSession
import com.tabbify.domain.scoring.ScoreConfig
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeRecentSessions(limit: Int = 20): Flow<List<PracticeSession>>
    suspend fun getSessionById(id: String): PracticeSession?
    suspend fun saveSession(session: PracticeSession): PracticeSession
    suspend fun deleteSession(id: String)
    suspend fun getTotalPracticeTimeMs(): Long
    suspend fun getCurrentStreak(): Int

    suspend fun getScoreConfig(instrument: InstrumentType): ScoreConfig?
    suspend fun saveScoreConfig(config: ScoreConfig)
}
