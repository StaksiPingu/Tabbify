package com.tabbify.platform

import com.tabbify.data.model.TrackAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

actual class AudioEngine actual constructor() {

    private val _amplitudeFlow = MutableSharedFlow<Float>(replay = 0, extraBufferCapacity = 64)
    actual val isRecording: Boolean = false
    actual val isPlaying: Boolean = false

    actual fun startRecording(outputPath: String) {
        // Web Audio API MediaRecorder via JS interop
        startWebRecording(outputPath)
    }

    actual fun stopRecording(): Long {
        stopWebRecording()
        return 0L
    }

    actual fun startPlayback(filePath: String) {
        startWebPlayback(filePath)
    }

    actual fun stopPlayback() {
        stopWebPlayback()
    }

    actual fun setVolume(filePath: String, volume: Float) {
        setWebVolume(filePath, volume)
    }

    actual fun getAmplitudeFlow(): Flow<Float> = _amplitudeFlow

    actual suspend fun analyzeAudio(filePath: String): TrackAnalysis {
        return TrackAnalysis(
            pitchAccuracy = 0.75f,
            rhythmConsistency = 0.80f,
            dynamicsRange = 0.65f,
            analyzedAt = System.currentTimeMillis()
        )
    }

    actual fun release() {}
}

private fun startWebRecording(outputPath: String) {
    js("""
        navigator.mediaDevices.getUserMedia({ audio: true }).then(function(stream) {
            window._tabbifyRecorder = new MediaRecorder(stream);
            window._tabbifyChunks = [];
            window._tabbifyRecorder.ondataavailable = function(e) {
                window._tabbifyChunks.push(e.data);
            };
            window._tabbifyRecorder.start();
        });
    """)
}

private fun stopWebRecording() {
    js("""
        if (window._tabbifyRecorder) {
            window._tabbifyRecorder.stop();
        }
    """)
}

private fun startWebPlayback(filePath: String) {
    js("""
        var audio = new Audio(filePath);
        window._tabbifyPlayers = window._tabbifyPlayers || {};
        window._tabbifyPlayers[filePath] = audio;
        audio.play();
    """)
}

private fun stopWebPlayback() {
    js("""
        if (window._tabbifyPlayers) {
            Object.values(window._tabbifyPlayers).forEach(function(a) { a.pause(); });
            window._tabbifyPlayers = {};
        }
    """)
}

private fun setWebVolume(filePath: String, volume: Float) {
    js("""
        if (window._tabbifyPlayers && window._tabbifyPlayers[filePath]) {
            window._tabbifyPlayers[filePath].volume = volume;
        }
    """)
}
