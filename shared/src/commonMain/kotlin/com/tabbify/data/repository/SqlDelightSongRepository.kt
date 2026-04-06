package com.tabbify.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.Song
import com.tabbify.data.model.Track
import com.tabbify.data.model.TrackAnalysis
import com.tabbify.db.TabbifyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SqlDelightSongRepository(
    private val db: TabbifyDatabase
) : SongRepository {

    private val songQueries = db.songsQueries

    override fun observeAllSongs(): Flow<List<Song>> {
        return songQueries.getAllSongs()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    val tracks = songQueries.getTracksBySongId(row.id)
                        .executeAsList()
                        .map { it.toTrack() }
                    row.toSong(tracks)
                }
            }
    }

    override suspend fun getSongById(id: String): Song? = withContext(Dispatchers.Default) {
        val row = songQueries.getSongById(id).executeAsOneOrNull() ?: return@withContext null
        val tracks = songQueries.getTracksBySongId(id).executeAsList().map { it.toTrack() }
        row.toSong(tracks)
    }

    override suspend fun createSong(song: Song): Song = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        val id = song.id.ifBlank { uuid4().toString() }
        songQueries.insertSong(
            id = id,
            title = song.title,
            bpm = song.bpm.toLong(),
            time_signature = song.timeSignature,
            instrument = song.instrument.name,
            created_at = now,
            updated_at = now,
            synced_at = null
        )
        song.copy(id = id, createdAt = now, updatedAt = now)
    }

    override suspend fun updateSong(song: Song): Unit = withContext(Dispatchers.Default) {
        songQueries.updateSong(
            title = song.title,
            bpm = song.bpm.toLong(),
            time_signature = song.timeSignature,
            instrument = song.instrument.name,
            updated_at = System.currentTimeMillis(),
            id = song.id
        )
    }

    override suspend fun deleteSong(id: String): Unit = withContext(Dispatchers.Default) {
        songQueries.deleteSong(id)
    }

    override suspend fun addTrackToSong(songId: String, track: Track): Track = withContext(Dispatchers.Default) {
        val id = track.id.ifBlank { uuid4().toString() }
        val now = System.currentTimeMillis()
        songQueries.insertTrack(
            id = id,
            song_id = songId,
            name = track.name,
            instrument = track.instrument.name,
            audio_file_path = track.audioFilePath,
            volume = track.volume.toDouble(),
            is_muted = if (track.isMuted) 1L else 0L,
            is_solo = if (track.isSolo) 1L else 0L,
            duration_ms = track.durationMs,
            created_at = now
        )
        track.copy(id = id, songId = songId, createdAt = now)
    }

    override suspend fun updateTrack(track: Track): Unit = withContext(Dispatchers.Default) {
        songQueries.updateTrack(
            name = track.name,
            volume = track.volume.toDouble(),
            is_muted = if (track.isMuted) 1L else 0L,
            is_solo = if (track.isSolo) 1L else 0L,
            duration_ms = track.durationMs,
            id = track.id
        )
    }

    override suspend fun deleteTrack(trackId: String): Unit = withContext(Dispatchers.Default) {
        songQueries.deleteTrack(trackId)
    }
}

// ── Extension mappers ─────────────────────────────────────────────────────────

private fun com.tabbify.db.Songs.toSong(tracks: List<Track>) = Song(
    id = id,
    title = title,
    bpm = bpm.toInt(),
    timeSignature = time_signature,
    instrument = runCatching { InstrumentType.valueOf(instrument) }.getOrDefault(InstrumentType.GUITAR),
    tracks = tracks,
    createdAt = created_at,
    updatedAt = updated_at,
    syncedAt = synced_at
)

private fun com.tabbify.db.Tracks.toTrack() = Track(
    id = id,
    songId = song_id,
    name = name,
    instrument = runCatching { InstrumentType.valueOf(instrument) }.getOrDefault(InstrumentType.GUITAR),
    audioFilePath = audio_file_path,
    volume = volume.toFloat(),
    isMuted = is_muted == 1L,
    isSolo = is_solo == 1L,
    durationMs = duration_ms,
    createdAt = created_at
)
