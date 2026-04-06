package com.tabbify.ui.songbuilder

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tabbify.data.model.Song
import com.tabbify.data.model.Track
import com.tabbify.data.repository.SongRepository
import com.tabbify.platform.AudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SongBuilderState(
    val song: Song = Song(id = "", title = "Song"),
    val isPlaying: Boolean = false
) {
    val songTitle: String get() = song.title
    val bpm: Int get() = song.bpm
    val timeSignature: String get() = song.timeSignature
    val tracks: List<Track> get() = song.tracks
}

class SongBuilderViewModel(
    private val songId: String,
    private val songRepo: SongRepository,
    private val audioEngine: AudioEngine = AudioEngine()
) : ScreenModel {

    private val _state = MutableStateFlow(SongBuilderState())
    val state: StateFlow<SongBuilderState> = _state

    init {
        screenModelScope.launch {
            val song = songRepo.getSongById(songId)
            if (song != null) _state.value = SongBuilderState(song = song)
        }
    }

    fun togglePlayback() {
        val isPlaying = _state.value.isPlaying
        if (isPlaying) {
            audioEngine.stopPlayback()
        } else {
            _state.value.tracks
                .filterNot { it.isMuted }
                .let { active ->
                    val soloTracks = active.filter { it.isSolo }
                    (if (soloTracks.isNotEmpty()) soloTracks else active)
                }
                .forEach { track ->
                    audioEngine.startPlayback(track.audioFilePath)
                    audioEngine.setVolume(track.audioFilePath, track.volume)
                }
        }
        _state.value = _state.value.copy(isPlaying = !isPlaying)
    }

    fun setVolume(trackId: String, volume: Float) {
        screenModelScope.launch {
            val track = _state.value.tracks.find { it.id == trackId } ?: return@launch
            audioEngine.setVolume(track.audioFilePath, volume)
            val updated = track.copy(volume = volume)
            songRepo.updateTrack(updated)
            updateLocalTrack(updated)
        }
    }

    fun toggleMute(trackId: String) {
        screenModelScope.launch {
            val track = _state.value.tracks.find { it.id == trackId } ?: return@launch
            val updated = track.copy(isMuted = !track.isMuted)
            songRepo.updateTrack(updated)
            updateLocalTrack(updated)
        }
    }

    fun toggleSolo(trackId: String) {
        screenModelScope.launch {
            val hasSolo = _state.value.tracks.any { it.id == trackId && it.isSolo }
            _state.value.tracks.forEach { track ->
                val newSolo = if (track.id == trackId) !hasSolo else false
                if (track.isSolo != newSolo) {
                    val updated = track.copy(isSolo = newSolo)
                    songRepo.updateTrack(updated)
                }
            }
            _state.value = _state.value.copy(
                song = _state.value.song.copy(
                    tracks = _state.value.tracks.map { t ->
                        t.copy(isSolo = if (t.id == trackId) !hasSolo else false)
                    }
                )
            )
        }
    }

    fun deleteTrack(trackId: String) {
        screenModelScope.launch {
            songRepo.deleteTrack(trackId)
            _state.value = _state.value.copy(
                song = _state.value.song.copy(
                    tracks = _state.value.tracks.filterNot { it.id == trackId }
                )
            )
        }
    }

    private fun updateLocalTrack(updated: Track) {
        _state.value = _state.value.copy(
            song = _state.value.song.copy(
                tracks = _state.value.tracks.map { if (it.id == updated.id) updated else it }
            )
        )
    }

    override fun onDispose() {
        audioEngine.stopPlayback()
        audioEngine.release()
    }
}
