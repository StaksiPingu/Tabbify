package com.tabbify.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.Song
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeStats(
    val songCount: Int = 0,
    val sessionCount: Int = 0,
    val streakDays: Int = 0,
    val avgScore: Float = 0f
)

class HomeViewModel : ScreenModel {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats

    init {
        loadSongs()
    }

    private fun loadSongs() {
        // TODO: Inject SongRepository via Koin and load from SQLDelight
        _songs.value = emptyList()
        _stats.value = HomeStats(songCount = _songs.value.size)
    }

    fun createSong(title: String, instrument: InstrumentType, bpm: Int) {
        screenModelScope.launch {
            val song = Song(
                id = uuid4().toString(),
                title = title,
                instrument = instrument,
                bpm = bpm,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            _songs.value = _songs.value + song
            _stats.value = _stats.value.copy(songCount = _songs.value.size)
        }
    }

    fun deleteSong(id: String) {
        screenModelScope.launch {
            _songs.value = _songs.value.filterNot { it.id == id }
            _stats.value = _stats.value.copy(songCount = _songs.value.size)
        }
    }
}
