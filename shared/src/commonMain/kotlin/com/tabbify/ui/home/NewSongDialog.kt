package com.tabbify.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tabbify.data.model.InstrumentType

@Composable
fun NewSongDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, instrument: InstrumentType, bpm: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedInstrument by remember { mutableStateOf(InstrumentType.GUITAR) }
    var bpm by remember { mutableStateOf(120f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Song erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    placeholder = { Text("z.B. Mein erstes Riff") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Instrument", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(InstrumentType.entries) { instrument ->
                        FilterChip(
                            selected = selectedInstrument == instrument,
                            onClick = { selectedInstrument = instrument },
                            label = { Text(instrument.emoji, style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("BPM: ${bpm.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 40f..280f,
                    steps = 239
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("40", style = MaterialTheme.typography.labelSmall)
                    Text("280", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title.trim(), selectedInstrument, bpm.toInt()) },
                enabled = title.isNotBlank()
            ) { Text("Erstellen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
