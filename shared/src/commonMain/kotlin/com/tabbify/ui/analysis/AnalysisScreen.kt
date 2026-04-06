package com.tabbify.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tabbify.data.model.InstrumentType
import com.tabbify.domain.scoring.DefaultScoreConfigs
import com.tabbify.domain.scoring.ScoreCriteria
import com.tabbify.ui.theme.TabbifySuccess
import com.tabbify.ui.theme.TabbifyWarning
import com.tabbify.ui.theme.TabbifyError

class AnalysisScreen(private val trackPath: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { AnalysisViewModel(trackPath) }
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Analyse & Score", fontWeight = FontWeight.Bold) },
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                if (state.isAnalyzing) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Analysiere Aufnahme…")
                        }
                    }
                } else {
                    state.scoreResult?.let { result ->
                        ScoreCircle(score = result.totalScore, grade = result.grade)
                        Text(
                            result.feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    state.analysis?.let { analysis ->
                        WaveformCard(waveform = analysis.waveformData)
                        CriteriaBreakdown(
                            criteriaScores = state.scoreResult?.criteriaScores ?: emptyMap(),
                            criteria = state.config?.criteria ?: emptyList()
                        )
                    }

                    ScoringConfigCard(
                        instrument = state.instrument,
                        onConfigureClick = { /* TODO: navigate to ScoringConfigScreen */ }
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ScoreCircle(score: Float, grade: String) {
    val color = when {
        score >= 80 -> TabbifySuccess
        score >= 60 -> TabbifyWarning
        else -> TabbifyError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * (score / 100f),
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${score.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("/ 100", style = MaterialTheme.typography.labelSmall)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(grade, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = color)
                Text("Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WaveformCard(waveform: List<Float>) {
    val barColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waveform", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                if (waveform.isEmpty()) return@Canvas
                val barWidth = size.width / waveform.size
                val centerY = size.height / 2
                waveform.forEachIndexed { i, amp ->
                    val barHeight = (amp * size.height * 0.8f).coerceAtLeast(2f)
                    drawLine(
                        color = barColor,
                        start = Offset(i * barWidth + barWidth / 2, centerY - barHeight / 2),
                        end = Offset(i * barWidth + barWidth / 2, centerY + barHeight / 2),
                        strokeWidth = barWidth * 0.7f
                    )
                }
            }
        }
    }
}

@Composable
private fun CriteriaBreakdown(
    criteriaScores: Map<String, Float>,
    criteria: List<ScoreCriteria>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kriterien", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            criteria.filter { it.enabled }.forEach { crit ->
                val score = criteriaScores[crit.id] ?: 0f
                val color = when {
                    score >= 0.8f -> TabbifySuccess
                    score >= 0.6f -> TabbifyWarning
                    else -> TabbifyError
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(crit.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { score },
                        modifier = Modifier.fillMaxWidth(),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoringConfigCard(instrument: InstrumentType, onConfigureClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bewertungskriterien", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${instrument.displayName} · Anpassen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onConfigureClick) {
                Icon(Icons.Default.Tune, "Konfigurieren")
            }
        }
    }
}
