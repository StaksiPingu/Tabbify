package com.tabbify.platform

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
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
import kotlin.math.log2
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

    // ── TarsosDSP Analysis ────────────────────────────────────────────────────

    actual suspend fun analyzeAudio(filePath: String): TrackAnalysis = withContext(Dispatchers.Default) {
        val sampleRate = 22050
        val bufferSize = 1024
        val overlap = 0

        val pitches = mutableListOf<Float>()
        val onsetTimestamps = mutableListOf<Double>()
        val rmsValues = mutableListOf<Float>()
        val waveform = mutableListOf<Float>()

        try {
            val dispatcher = AudioDispatcherFactory.fromPipe(filePath, sampleRate, bufferSize, overlap)

            // Pitch detection via YIN algorithm
            val pitchHandler = PitchDetectionHandler { result: PitchDetectionResult, _: AudioEvent ->
                val pitch = result.pitch
                if (pitch > 0 && result.probability > 0.8f) {
                    pitches.add(pitch)
                }
            }
            dispatcher.addAudioProcessor(
                PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate.toFloat(), bufferSize, pitchHandler)
            )

            // Onset detection for BPM estimation
            val onsetDetector = ComplexOnsetDetector(bufferSize)
            onsetDetector.setHandler(OnsetHandler { time, _ ->
                onsetTimestamps.add(time)
            })
            dispatcher.addAudioProcessor(onsetDetector)

            // RMS & waveform extraction
            dispatcher.addAudioProcessor(object : be.tarsos.dsp.AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    val buffer = audioEvent.floatBuffer
                    var rmsSum = 0.0
                    var peak = 0f
                    buffer.forEach { s ->
                        rmsSum += s * s
                        if (abs(s) > peak) peak = abs(s)
                    }
                    val rms = sqrt(rmsSum / buffer.size).toFloat()
                    rmsValues.add(rms)
                    waveform.add(peak)
                    return true
                }
                override fun processingFinished() {}
            })

            dispatcher.run()
        } catch (_: Exception) {}

        // ── Calculate metrics ─────────────────────────────────────────────────

        val pitchAccuracy = calculatePitchAccuracy(pitches)
        val detectedBpm = estimateBpm(onsetTimestamps)
        val rhythmConsistency = calculateRhythmConsistency(onsetTimestamps, detectedBpm)
        val avgRms = if (rmsValues.isNotEmpty()) rmsValues.average().toFloat() else 0f
        val dynamicsRange = (avgRms * 4f).coerceIn(0f, 1f)

        // Downsample waveform to 100 points
        val downsampled = downsampleWaveform(waveform, 100)

        TrackAnalysis(
            averagePitch = if (pitches.isNotEmpty()) pitches.average().toFloat() else 0f,
            pitchAccuracy = pitchAccuracy,
            rhythmConsistency = rhythmConsistency,
            dynamicsRange = dynamicsRange,
            detectedBpm = detectedBpm,
            waveformData = downsampled,
            analyzedAt = System.currentTimeMillis()
        )
    }

    private fun calculatePitchAccuracy(pitches: List<Float>): Float {
        if (pitches.size < 4) return 0.5f

        // Convert Hz → MIDI notes, measure consistency around median
        val midiNotes = pitches.map { hz ->
            (69 + 12 * log2(hz / 440.0)).toFloat()
        }
        val sorted = midiNotes.sorted()
        val median = sorted[sorted.size / 2]
        val deviations = midiNotes.map { abs(it - median) }
        val avgDeviation = deviations.average().toFloat()

        // Score: 0.5 semitone deviation = ~0.8 accuracy, 2 semitones = ~0.3
        return (1f - (avgDeviation / 3f).coerceIn(0f, 0.9f)).coerceIn(0.1f, 1.0f)
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
