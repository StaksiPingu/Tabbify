package com.tabbify.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.benasher44.uuid.uuid4
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.Song
import com.tabbify.data.repository.SessionRepository
import com.tabbify.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeStats(
    val songCount: Int = 0,
    val sessionCount: Int = 0,
    val streakDays: Int = 0,
    val avgScore: Float = 0f,
    val totalPracticeMinutes: Long = 0L
)

class HomeViewModel(
    private val songRepo: SongRepository,
    private val sessionRepo: SessionRepository
) : ScreenModel {

    val songs: StateFlow<List<Song>> = songRepo
        .observeAllSongs()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats

    init {
        loadStats()
    }

    private fun loadStats() {
        screenModelScope.launch {
            val totalMs = sessionRepo.getTotalPracticeTimeMs()
            val streak = sessionRepo.getCurrentStreak()
            _stats.value = HomeStats(
                songCount = songs.value.size,
                streakDays = streak,
                totalPracticeMinutes = totalMs / 60_000
            )
        }
    }

    fun createSong(title: String, instrument: InstrumentType, bpm: Int) {
        screenModelScope.launch {
            songRepo.createSong(
                Song(
                    id = uuid4().toString(),
                    title = title,
                    instrument = instrument,
                    bpm = bpm,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteSong(id: String) {
        screenModelScope.launch {
            songRepo.deleteSong(id)
        }
    }
}
