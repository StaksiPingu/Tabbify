package com.tabbify.ui.recorder

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tabbify.ui.analysis.AnalysisScreen
import com.tabbify.ui.theme.TabbifyError

class RecorderScreen(private val songId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { RecorderViewModel(songId) }
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Aufnahme", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Zurück")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(16.dp))

                // Timer
                RecordingTimer(
                    durationMs = state.durationMs,
                    isRecording = state.isRecording
                )

                // Waveform Visualizer
                WaveformVisualizer(
                    amplitudes = state.waveform,
                    isRecording = state.isRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Take Hinweis
                if (state.lastRecordingPath != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Letzte Aufnahme", style = MaterialTheme.typography.labelMedium)
                                Text("Take ${state.takeCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Row {
                                IconButton(onClick = { viewModel.playLastRecording() }) {
                                    Icon(Icons.Default.PlayArrow, "Abspielen")
                                }
                                IconButton(onClick = {
                                    state.lastRecordingPath?.let {
                                        navigator.push(AnalysisScreen(it))
                                    }
                                }) {
                                    Icon(Icons.Default.Analytics, "Analysieren")
                                }
                            }
                        }
                    }
                }

                // Record Button
                RecordButton(
                    isRecording = state.isRecording,
                    onStartRecord = { viewModel.startRecording() },
                    onStopRecord = { viewModel.stopRecording() }
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RecordingTimer(durationMs: Long, isRecording: Boolean) {
    val seconds = (durationMs / 1000) % 60
    val minutes = durationMs / 60000

    val pulse by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) TabbifyError else MaterialTheme.colorScheme.onBackground
        )
        if (isRecording) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TabbifyError)
                )
                Text("REC", style = MaterialTheme.typography.labelSmall, color = TabbifyError)
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val barCount = 60
        val barWidth = size.width / barCount
        val centerY = size.height / 2

        repeat(barCount) { i ->
            val amplitude = amplitudes.getOrElse(i) { 0f }
            val barHeight = (amplitude * size.height * 0.8f).coerceAtLeast(4f)
            val color = if (isRecording) primaryColor else surfaceColor

            drawLine(
                color = color,
                start = Offset(i * barWidth + barWidth / 2, centerY - barHeight / 2),
                end = Offset(i * barWidth + barWidth / 2, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.6f
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit
) {
    val size by animateDpAsState(
        targetValue = if (isRecording) 80.dp else 72.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "recordButtonSize"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        FloatingActionButton(
            onClick = if (isRecording) onStopRecord else onStartRecord,
            modifier = Modifier.size(size),
            containerColor = if (isRecording) TabbifyError else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Aufnahme stoppen" else "Aufnahme starten",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
