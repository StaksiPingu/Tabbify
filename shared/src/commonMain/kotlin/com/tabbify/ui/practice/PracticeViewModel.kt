package com.tabbify.ui.practice

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MetronomeState(
    val bpm: Int = 120,
    val beatsPerMeasure: Int = 4,
    val timeSignature: String = "4/4",
    val isPlaying: Boolean = false,
    val currentBeat: Int = 0
)

data class TunerState(
    val detectedNote: String? = null,
    val frequency: Float? = null,
    val centsOff: Int? = null,
    val isListening: Boolean = false
)

data class TimerState(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 30
)

class PracticeViewModel : ScreenModel {

    private val _metronomeState = MutableStateFlow(MetronomeState())
    val metronomeState: StateFlow<MetronomeState> = _metronomeState

    private val _tunerState = MutableStateFlow(TunerState())
    val tunerState: StateFlow<TunerState> = _tunerState

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState

    private var metronomeJob: Job? = null
    private var timerJob: Job? = null
    private var timerStart = 0L

    // ── Metronome ──────────────────────────────────────────────────────────────

    fun setBpm(bpm: Int) {
        _metronomeState.value = _metronomeState.value.copy(bpm = bpm.coerceIn(40, 280))
        if (_metronomeState.value.isPlaying) {
            restartMetronome()
        }
    }

    fun setTimeSignature(sig: String) {
        val beats = sig.split("/").firstOrNull()?.toIntOrNull() ?: 4
        _metronomeState.value = _metronomeState.value.copy(timeSignature = sig, beatsPerMeasure = beats)
    }

    fun toggleMetronome() {
        if (_metronomeState.value.isPlaying) stopMetronome() else startMetronome()
    }

    private fun startMetronome() {
        _metronomeState.value = _metronomeState.value.copy(isPlaying = true, currentBeat = 0)
        restartMetronome()
    }

    private fun restartMetronome() {
        metronomeJob?.cancel()
        metronomeJob = screenModelScope.launch {
            while (true) {
                val state = _metronomeState.value
                val intervalMs = (60_000L / state.bpm)
                val nextBeat = (state.currentBeat + 1) % state.beatsPerMeasure
                _metronomeState.value = state.copy(currentBeat = nextBeat)
                // TODO: Play click sound via AudioEngine (downbeat vs. regular beat)
                delay(intervalMs)
            }
        }
    }

    private fun stopMetronome() {
        metronomeJob?.cancel()
        _metronomeState.value = _metronomeState.value.copy(isPlaying = false, currentBeat = 0)
    }

    // ── Tuner ─────────────────────────────────────────────────────────────────

    fun toggleTuner() {
        if (_tunerState.value.isListening) stopTuner() else startTuner()
    }

    private fun startTuner() {
        _tunerState.value = TunerState(isListening = true)
        screenModelScope.launch {
            // TODO: Use AudioEngine pitch detection loop
            // Simulate for now
            delay(1000)
            _tunerState.value = TunerState(
                isListening = true,
                detectedNote = "E4",
                frequency = 329.6f,
                centsOff = -3
            )
        }
    }

    private fun stopTuner() {
        _tunerState.value = TunerState(isListening = false)
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    fun toggleTimer() {
        if (_timerState.value.isRunning) {
            timerJob?.cancel()
            _timerState.value = _timerState.value.copy(isRunning = false)
        } else {
            timerStart = System.currentTimeMillis() - _timerState.value.elapsedMs
            _timerState.value = _timerState.value.copy(isRunning = true)
            timerJob = screenModelScope.launch {
                while (true) {
                    delay(100)
                    val elapsed = System.currentTimeMillis() - timerStart
                    val todayMin = (elapsed / 60_000).toInt()
                    _timerState.value = _timerState.value.copy(
                        elapsedMs = elapsed,
                        todayMinutes = todayMin
                    )
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState(goalMinutes = _timerState.value.goalMinutes)
    }

    override fun onDispose() {
        metronomeJob?.cancel()
        timerJob?.cancel()
    }
}
