package com.tabbify.data.repository

import com.tabbify.data.model.Song
import com.tabbify.data.model.Track
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun observeAllSongs(): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
    suspend fun createSong(song: Song): Song
    suspend fun updateSong(song: Song)
    suspend fun deleteSong(id: String)

    suspend fun addTrackToSong(songId: String, track: Track): Track
    suspend fun updateTrack(track: Track)
    suspend fun deleteTrack(trackId: String)
}
