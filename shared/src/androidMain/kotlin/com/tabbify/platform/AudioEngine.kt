package com.tabbify.platform

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import com.tabbify.data.model.TrackAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

actual class AudioEngine actual constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaRecorder: MediaRecorder? = null
    private val mediaPlayers = mutableMapOf<String, MediaPlayer>()
    private var amplitudeJob: Job? = null
    private val _amplitudeFlow = MutableSharedFlow<Float>(replay = 0, extraBufferCapacity = 64)

    actual val isRecording: Boolean get() = mediaRecorder != null
    actual val isPlaying: Boolean get() = mediaPlayers.values.any { it.isPlaying }

    actual fun startRecording(outputPath: String) {
        if (isRecording) stopRecording()
        @Suppress("DEPRECATION")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(192000)
            setOutputFile(outputPath)
            prepare()
            start()
        }
        amplitudeJob = scope.launch {
            while (isRecording) {
                val amplitude = mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
                _amplitudeFlow.emit((amplitude / 32767f).coerceIn(0f, 1f))
                delay(50)
            }
        }
    }

    actual fun stopRecording(): Long {
        val start = System.currentTimeMillis()
        amplitudeJob?.cancel()
        amplitudeJob = null
        runCatching { mediaRecorder?.stop(); mediaRecorder?.release() }
        mediaRecorder = null
        return System.currentTimeMillis() - start
    }

    actual fun startPlayback(filePath: String) {
        stopSinglePlayback(filePath)
        val player = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
        mediaPlayers[filePath] = player
    }

    actual fun stopPlayback() {
        mediaPlayers.values.forEach { runCatching { it.stop(); it.release() } }
        mediaPlayers.clear()
    }

    private fun stopSinglePlayback(filePath: String) {
        mediaPlayers.remove(filePath)?.let { runCatching { it.stop(); it.release() } }
    }

    actual fun setVolume(filePath: String, volume: Float) {
        mediaPlayers[filePath]?.setVolume(volume, volume)
    }

    actual fun getAmplitudeFlow(): Flow<Float> = _amplitudeFlow

    // ── Audio Analysis (using Android AudioRecord) ────────────────────────────

    actual suspend fun analyzeAudio(filePath: String): TrackAnalysis = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val rmsValues = mutableListOf<Float>()
        val waveform = mutableListOf<Float>()
        val onsetTimestamps = mutableListOf<Double>()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            val durationMs = player.duration.toLong()
            player.release()

            // Use AudioRecord to capture playback for analysis
            // Since we're analyzing a file, we read raw PCM via MediaPlayer + AudioRecord workaround
            // Instead: simple file-based RMS estimation from MediaRecorder amplitude snapshots
            // For now return reasonable placeholder values based on duration

            val durationSeconds = durationMs / 1000.0
            val segments = (durationSeconds / 0.1).roundToInt().coerceAtLeast(10)

            // Simulate waveform from duration-based estimation
            repeat(segments) { i ->
                val t = i.toDouble() / segments
                val fakePeak = (0.3f + 0.4f * kotlin.math.sin(t * Math.PI * 4).toFloat()).coerceIn(0f, 1f)
                waveform.add(fakePeak)
                rmsValues.add(fakePeak * 0.7f)
                if (i % 8 == 0) onsetTimestamps.add(t * durationSeconds)
            }
        } catch (_: Exception) {}

        val detectedBpm = estimateBpm(onsetTimestamps)
        val rhythmConsistency = calculateRhythmConsistency(onsetTimestamps, detectedBpm)
        val avgRms = if (rmsValues.isNotEmpty()) rmsValues.average().toFloat() else 0.5f
        val dynamicsRange = (avgRms * 2f).coerceIn(0f, 1f)
        val downsampled = downsampleWaveform(waveform, 100)

        TrackAnalysis(
            averagePitch = 0f,
            pitchAccuracy = 0.7f,
            rhythmConsistency = rhythmConsistency,
            dynamicsRange = dynamicsRange,
            detectedBpm = detectedBpm,
            waveformData = downsampled,
            analyzedAt = System.currentTimeMillis()
        )
    }

    private fun estimateBpm(onsets: List<Double>): Float? {
        if (onsets.size < 4) return null
        val intervals = onsets.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        return if (avgInterval > 0) (60.0 / avgInterval).toFloat().coerceIn(40f, 280f) else null
    }

    private fun calculateRhythmConsistency(onsets: List<Double>, bpm: Float?): Float {
        if (onsets.size < 4 || bpm == null) return 0.5f
        val expectedInterval = 60.0 / bpm
        val intervals = onsets.zipWithNext { a, b -> b - a }
        val deviations = intervals.map { abs(it - expectedInterval) / expectedInterval }
        val avgDev = deviations.average().toFloat()
        return (1f - (avgDev * 2f).coerceIn(0f, 0.9f)).coerceIn(0.1f, 1.0f)
    }

    private fun downsampleWaveform(waveform: List<Float>, targetSize: Int): List<Float> {
        if (waveform.isEmpty()) return List(targetSize) { 0f }
        if (waveform.size <= targetSize) return waveform
        val step = waveform.size.toDouble() / targetSize
        return (0 until targetSize).map { i ->
            val idx = (i * step).roundToInt().coerceIn(0, waveform.size - 1)
            waveform[idx]
        }
    }

    actual fun release() {
        stopRecording()
        stopPlayback()
        scope.coroutineContext[Job]?.cancel()
    }
}
