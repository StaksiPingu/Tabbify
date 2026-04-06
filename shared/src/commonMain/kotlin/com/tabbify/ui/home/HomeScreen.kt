package com.tabbify.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tabbify.data.model.Song
import com.tabbify.ui.practice.PracticeScreen
import com.tabbify.ui.settings.SettingsScreen
import com.tabbify.ui.recorder.RecorderScreen
import com.tabbify.ui.songbuilder.SongBuilderScreen

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = remember { HomeViewModel() }
        val songs by viewModel.songs.collectAsState()
        val stats by viewModel.stats.collectAsState()
        var showNewSongDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tabbify", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { navigator.push(PracticeScreen()) }) {
                            Icon(Icons.Default.MusicNote, contentDescription = "Übungstools")
                        }
                        IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showNewSongDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Neuen Song erstellen")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                StatsCard(stats = stats)

                Spacer(Modifier.height(8.dp))

                if (songs.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        onCreateSong = { showNewSongDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SongCard(
                                song = song,
                                onClick = { navigator.push(SongBuilderScreen(song.id)) },
                                onRecord = { navigator.push(RecorderScreen(song.id)) },
                                onDelete = { viewModel.deleteSong(song.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showNewSongDialog) {
            NewSongDialog(
                onDismiss = { showNewSongDialog = false },
                onCreate = { title, instrument, bpm ->
                    viewModel.createSong(title, instrument, bpm)
                    showNewSongDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatsCard(stats: HomeStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Songs", value = stats.songCount.toString(), icon = Icons.Default.LibraryMusic)
            StatItem(label = "Sessions", value = stats.sessionCount.toString(), icon = Icons.Default.FitnessCenter)
            StatItem(label = "Streak", value = "${stats.streakDays}T", icon = Icons.Default.LocalFire)
            StatItem(label = "Ø Score", value = if (stats.avgScore > 0) "${stats.avgScore.toInt()}%" else "-", icon = Icons.Default.Star)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onRecord: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(song.instrument.emoji, style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${song.instrument.displayName} · ${song.bpm} BPM · ${song.timeSignature} · ${song.tracks.size} Spuren",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRecord) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = "Aufnehmen", tint = MaterialTheme.colorScheme.error)
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Song öffnen") },
                        onClick = { expanded = false; onClick() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onCreateSong: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎸", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Noch keine Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Erstelle deinen ersten Song und fang an zu üben!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateSong) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Song erstellen")
        }
    }
}
