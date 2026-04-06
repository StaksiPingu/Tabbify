package com.tabbify.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tabbify.ui.theme.TabbifySuccess
import com.tabbify.ui.theme.TabbifyWarning
import com.tabbify.ui.theme.TabbifyError

class PracticeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { PracticeViewModel() }
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Übungstools", fontWeight = FontWeight.Bold) },
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
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Metronom") }, icon = { Icon(Icons.Default.GraphicEq, null, Modifier.size(18.dp)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tuner") }, icon = { Icon(Icons.Default.Tune, null, Modifier.size(18.dp)) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Akkorde") }, icon = { Icon(Icons.Default.LibraryMusic, null, Modifier.size(18.dp)) })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Timer") }, icon = { Icon(Icons.Default.Timer, null, Modifier.size(18.dp)) })
                }

                when (selectedTab) {
                    0 -> MetronomeTab(viewModel = viewModel)
                    1 -> TunerTab(viewModel = viewModel)
                    2 -> ChordTab()
                    3 -> TimerTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun MetronomeTab(viewModel: PracticeViewModel) {
    val state by viewModel.metronomeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("${state.bpm}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text("BPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Slider(
            value = state.bpm.toFloat(),
            onValueChange = { viewModel.setBpm(it.toInt()) },
            valueRange = 40f..280f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("40 BPM", style = MaterialTheme.typography.labelSmall)
            Text("280 BPM", style = MaterialTheme.typography.labelSmall)
        }

        // Beat visualizer
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(state.beatsPerMeasure) { i ->
                val isActive = state.isPlaying && i == state.currentBeat
                val isDownbeat = i == 0
                Box(
                    modifier = Modifier
                        .size(if (isDownbeat) 24.dp else 20.dp)
                        .let {
                            if (isActive) it else it
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = when {
                                isActive && isDownbeat -> Color(0xFFD0BCFF)
                                isActive -> Color(0xFF9E8FC0)
                                else -> Color.Gray.copy(alpha = 0.4f)
                            }
                        )
                    }
                }
            }
        }

        // Time signature
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Takt:", style = MaterialTheme.typography.bodyMedium)
            listOf("2/4", "3/4", "4/4", "6/8").forEach { sig ->
                FilterChip(
                    selected = state.timeSignature == sig,
                    onClick = { viewModel.setTimeSignature(sig) },
                    label = { Text(sig) }
                )
            }
        }

        FloatingActionButton(
            onClick = { viewModel.toggleMetronome() },
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Stopp" else "Start",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun TunerTab(viewModel: PracticeViewModel) {
    val state by viewModel.tunerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Note display
        Text(
            state.detectedNote ?: "—",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = when {
                state.centsOff == null -> MaterialTheme.colorScheme.onBackground
                kotlin.math.abs(state.centsOff) <= 5 -> TabbifySuccess
                kotlin.math.abs(state.centsOff) <= 15 -> TabbifyWarning
                else -> TabbifyError
            }
        )

        state.centsOff?.let { cents ->
            Text(
                when {
                    cents > 0 -> "+$cents Cent (zu hoch)"
                    cents < 0 -> "$cents Cent (zu tief)"
                    else -> "Perfekt gestimmt!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tuner needle
        TunerNeedle(centsOff = state.centsOff ?: 0)

        Button(onClick = { viewModel.toggleTuner() }) {
            Icon(if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.isListening) "Tuner stoppen" else "Tuner starten")
        }

        // Standard tuning reference
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("E2", "A2", "D3", "G3", "B3", "E4").forEachIndexed { i, note ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${i + 1}.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(note, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TunerNeedle(centsOff: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height * 0.9f
        val radius = size.width * 0.4f

        // Arc background
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = 8f)
        )

        // Center line
        drawLine(
            color = TabbifySuccess,
            start = Offset(centerX, centerY - radius * 0.6f),
            end = Offset(centerX, centerY - radius),
            strokeWidth = 3f
        )

        // Needle
        val angle = (centsOff.coerceIn(-50, 50) / 50f) * 80f
        val angleRad = (180f + angle) * (Math.PI / 180f)
        val needleX = centerX + radius * 0.85f * kotlin.math.cos(angleRad).toFloat()
        val needleY = centerY + radius * 0.85f * kotlin.math.sin(angleRad).toFloat()

        drawLine(
            color = primary,
            start = Offset(centerX, centerY),
            end = Offset(needleX, needleY),
            strokeWidth = 4f
        )
        drawCircle(color = primary, radius = 8f, center = Offset(centerX, centerY))
    }
}

@Composable
private fun ChordTab() {
    val chords = mapOf(
        "Am" to listOf("x", "0", "2", "2", "1", "0"),
        "C" to listOf("x", "3", "2", "0", "1", "0"),
        "D" to listOf("x", "x", "0", "2", "3", "2"),
        "E" to listOf("0", "2", "2", "1", "0", "0"),
        "Em" to listOf("0", "2", "2", "0", "0", "0"),
        "F" to listOf("1", "1", "2", "3", "3", "1"),
        "G" to listOf("3", "2", "0", "0", "0", "3"),
        "G7" to listOf("3", "2", "0", "0", "0", "1")
    )
    var search by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Akkord suchen") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val filtered = chords.filter { it.key.contains(search, ignoreCase = true) }

        filtered.forEach { (name, frets) ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        frets.forEachIndexed { i, fret ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    listOf("E", "A", "D", "G", "B", "e")[i],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(fret, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerTab(viewModel: PracticeViewModel) {
    val state by viewModel.timerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val minutes = state.elapsedMs / 60000
        val seconds = (state.elapsedMs / 1000) % 60

        Text(
            "%02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp
        )

        Text(
            "Heute: ${state.todayMinutes} Min geübt",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.goalMinutes > 0) {
            LinearProgressIndicator(
                progress = { (state.todayMinutes / state.goalMinutes.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Tagesziel: ${state.goalMinutes} Min", style = MaterialTheme.typography.labelMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { viewModel.resetTimer() }) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }
            FloatingActionButton(onClick = { viewModel.toggleTimer() }, modifier = Modifier.size(72.dp)) {
                Icon(
                    if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
