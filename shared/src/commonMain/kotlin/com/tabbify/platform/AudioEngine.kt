package com.tabbify.platform

import com.tabbify.data.model.TrackAnalysis
import kotlinx.coroutines.flow.Flow

expect class AudioEngine() {
    val isRecording: Boolean
    val isPlaying: Boolean

    fun startRecording(outputPath: String)
    fun stopRecording(): Long

    fun startPlayback(filePath: String)
    fun stopPlayback()
    fun setVolume(filePath: String, volume: Float)

    fun getAmplitudeFlow(): Flow<Float>

    suspend fun analyzeAudio(filePath: String): TrackAnalysis

    fun release()
}
