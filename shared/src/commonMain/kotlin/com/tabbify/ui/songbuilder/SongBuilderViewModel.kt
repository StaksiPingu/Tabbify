package com.tabbify.ui.songbuilder

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tabbify.data.model.Track
import com.tabbify.platform.AudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SongBuilderState(
    val songTitle: String = "Song",
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val tracks: List<Track> = emptyList(),
    val isPlaying: Boolean = false
)

class SongBuilderViewModel(
    private val songId: String,
    private val audioEngine: AudioEngine = AudioEngine()
) : ScreenModel {

    private val _state = MutableStateFlow(SongBuilderState())
    val state: StateFlow<SongBuilderState> = _state

    init {
        loadSong()
    }

    private fun loadSong() {
        // TODO: Load from SongRepository
        _state.value = SongBuilderState(songTitle = "Mein Song", bpm = 120)
    }

    fun togglePlayback() {
        val isPlaying = _state.value.isPlaying
        if (isPlaying) {
            audioEngine.stopPlayback()
        } else {
            _state.value.tracks
                .filterNot { it.isMuted }
                .forEach { track -> audioEngine.startPlayback(track.audioFilePath) }
        }
        _state.value = _state.value.copy(isPlaying = !isPlaying)
    }

    fun setVolume(trackId: String, volume: Float) {
        screenModelScope.launch {
            val track = _state.value.tracks.find { it.id == trackId } ?: return@launch
            audioEngine.setVolume(track.audioFilePath, volume)
            _state.value = _state.value.copy(
                tracks = _state.value.tracks.map {
                    if (it.id == trackId) it.copy(volume = volume) else it
                }
            )
        }
    }

    fun toggleMute(trackId: String) {
        _state.value = _state.value.copy(
            tracks = _state.value.tracks.map {
                if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
            }
        )
    }

    fun toggleSolo(trackId: String) {
        val hasSolo = _state.value.tracks.any { it.id == trackId && it.isSolo }
        _state.value = _state.value.copy(
            tracks = _state.value.tracks.map {
                if (it.id == trackId) it.copy(isSolo = !hasSolo)
                else it.copy(isSolo = false)
            }
        )
    }

    fun deleteTrack(trackId: String) {
        _state.value = _state.value.copy(
            tracks = _state.value.tracks.filterNot { it.id == trackId }
        )
    }

    override fun onDispose() {
        audioEngine.stopPlayback()
        audioEngine.release()
    }
}
