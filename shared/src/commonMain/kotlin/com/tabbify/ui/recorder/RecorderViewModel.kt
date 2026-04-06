package com.tabbify.ui.recorder

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.benasher44.uuid.uuid4
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.Track
import com.tabbify.data.repository.SongRepository
import com.tabbify.platform.AudioEngine
import com.tabbify.platform.StorageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecorderState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val waveform: List<Float> = List(60) { 0f },
    val lastRecordingPath: String? = null,
    val lastTrackId: String? = null,
    val takeCount: Int = 0
)

class RecorderViewModel(
    private val songId: String,
    private val audioEngine: AudioEngine = AudioEngine(),
    private val storageManager: StorageManager = StorageManager(),
    private val songRepo: SongRepository? = null
) : ScreenModel {

    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state

    private var timerJob: Job? = null
    private var waveformJob: Job? = null
    private var startTime = 0L
    private var currentTrackId = ""

    fun startRecording() {
        currentTrackId = uuid4().toString()
        val path = storageManager.newRecordingPath(songId, currentTrackId)

        audioEngine.startRecording(path)
        startTime = System.currentTimeMillis()

        _state.value = _state.value.copy(isRecording = true, durationMs = 0L)

        timerJob = screenModelScope.launch {
            while (true) {
                delay(100)
                _state.value = _state.value.copy(
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }

        waveformJob = screenModelScope.launch {
            val buffer = ArrayDeque<Float>(60).also { q -> repeat(60) { q.add(0f) } }
            audioEngine.getAmplitudeFlow().collect { amplitude ->
                if (buffer.size >= 60) buffer.removeFirst()
                buffer.addLast(amplitude)
                _state.value = _state.value.copy(waveform = buffer.toList())
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        waveformJob?.cancel()
        audioEngine.stopRecording()

        val duration = _state.value.durationMs
        val path = storageManager.newRecordingPath(songId, currentTrackId)
        val takeNum = _state.value.takeCount + 1

        _state.value = _state.value.copy(
            isRecording = false,
            lastRecordingPath = path,
            lastTrackId = currentTrackId,
            takeCount = takeNum,
            waveform = List(60) { 0f }
        )

        // Persist track in database
        screenModelScope.launch {
            songRepo?.addTrackToSong(
                songId = songId,
                track = Track(
                    id = currentTrackId,
                    songId = songId,
                    name = "Take $takeNum",
                    instrument = InstrumentType.GUITAR,
                    audioFilePath = path,
                    durationMs = duration,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun playLastRecording() {
        _state.value.lastRecordingPath?.let { path ->
            audioEngine.startPlayback(path)
        }
    }

    override fun onDispose() {
        timerJob?.cancel()
        waveformJob?.cancel()
        audioEngine.release()
    }
}
