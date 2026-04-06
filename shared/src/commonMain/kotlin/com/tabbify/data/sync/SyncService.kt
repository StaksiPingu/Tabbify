package com.tabbify.data.sync

import com.tabbify.data.model.InstrumentType
import com.tabbify.data.remote.CreateSessionBody
import com.tabbify.data.remote.CreateSongBody
import com.tabbify.data.remote.TabbifyApiClient
import com.tabbify.data.repository.SessionRepository
import com.tabbify.data.repository.SongRepository

/**
 * Offline-first Cloud-Sync: pushes local-only data to the server.
 * Called by WorkManager on Android (periodic + on network available).
 */
class SyncService(
    private val apiClient: TabbifyApiClient,
    private val songRepo: SongRepository,
    private val sessionRepo: SessionRepository
) {
    suspend fun syncAll() {
        syncSongs()
        syncSessions()
    }

    private suspend fun syncSongs() {
        val localSongs = songRepo.observeAllSongs().let {
            // Collect one shot — in a real impl use first() from coroutines
            emptyList() // placeholder; actual impl uses Flow.first()
        }

        // Push unsynced songs
        localSongs.filter { it.syncedAt == null }.forEach { song ->
            val result = apiClient.createSong(
                CreateSongBody(
                    title = song.title,
                    bpm = song.bpm,
                    timeSignature = song.timeSignature,
                    instrument = song.instrument.name
                )
            )
            if (result.isSuccess) {
                songRepo.updateSong(song.copy(syncedAt = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun syncSessions() {
        val sessions = emptyList<com.tabbify.data.model.PracticeSession>() // placeholder

        sessions.filter { it.syncedAt == null }.forEach { session ->
            val result = apiClient.createSession(
                CreateSessionBody(
                    songId = session.songId,
                    instrument = session.instrument.name,
                    durationMs = session.durationMs,
                    score = session.score,
                    criteriaScores = session.criteriaScores,
                    notes = session.notes
                )
            )
            if (result.isSuccess) {
                sessionRepo.saveSession(session.copy(syncedAt = System.currentTimeMillis()))
            }
        }
    }
}
