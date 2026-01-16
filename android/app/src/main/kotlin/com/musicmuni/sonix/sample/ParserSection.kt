package com.musicmuni.sonix.sample

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.vozos.sonix.util.Parser
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ParserSection(context: Context) {
    val scope = rememberCoroutineScope()

    var pitchSummary by remember { mutableStateOf<String?>(null) }
    var notesSummary by remember { mutableStateOf<String?>(null) }
    var transSummary by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Ready") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📄 File Parsers",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Parse buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Button(
                onClick = {
                    scope.launch {
                        status = "Parsing pitch file..."
                        pitchSummary = null
                        try {
                            val content = withContext(Dispatchers.IO) {
                                context.assets.open("Alankaar 01.pitchPP")
                                    .bufferedReader()
                                    .readText()
                            }
                            val data = Parser.parsePitchString(content)
                            if (data != null) {
                                val minTime = data.times.minOrNull() ?: 0f
                                val maxTime = data.times.maxOrNull() ?: 0f
                                val validPitches = data.pitchesHz.filter { it > 0 }
                                val minPitch = validPitches.minOrNull() ?: 0f
                                val maxPitch = validPitches.maxOrNull() ?: 0f
                                pitchSummary = """
                                    |Pitch Data (.pitchPP):
                                    |  Points: ${data.count}
                                    |  Time range: %.2f - %.2f sec
                                    |  Pitch range: %.1f - %.1f Hz
                                    |  Voiced: ${validPitches.size} points
                                """.trimMargin().format(minTime, maxTime, minPitch, maxPitch)
                                status = "Pitch file parsed"
                            } else {
                                status = "Failed to parse pitch file"
                            }
                        } catch (e: Exception) {
                            Napier.e("Parse error", e)
                            status = "Error: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Parse .pitchPP")
            }

            Button(
                onClick = {
                    scope.launch {
                        status = "Parsing notes file..."
                        notesSummary = null
                        try {
                            val content = withContext(Dispatchers.IO) {
                                context.assets.open("Alankaar 01.notes")
                                    .bufferedReader()
                                    .readText()
                            }
                            val data = Parser.parseNotesString(content)
                            if (data != null) {
                                val uniqueLabels = data.labels.toSet()
                                val totalDuration = data.endTimes.maxOrNull()?.minus(
                                    data.startTimes.minOrNull() ?: 0f
                                ) ?: 0f
                                notesSummary = """
                                    |Notes Data (.notes):
                                    |  Notes: ${data.count}
                                    |  Duration: %.2f sec
                                    |  Unique svaras: ${uniqueLabels.size}
                                    |  Labels: ${uniqueLabels.joinToString(", ")}
                                """.trimMargin().format(totalDuration)
                                status = "Notes file parsed"
                            } else {
                                status = "Failed to parse notes file"
                            }
                        } catch (e: Exception) {
                            Napier.e("Parse error", e)
                            status = "Error: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Parse .notes")
            }

            Button(
                onClick = {
                    scope.launch {
                        status = "Parsing trans file..."
                        transSummary = null
                        try {
                            val content = withContext(Dispatchers.IO) {
                                context.assets.open("Alankaar 01.trans")
                                    .bufferedReader()
                                    .readText()
                            }
                            val data = Parser.parseTransString(content)
                            if (data != null) {
                                val totalNotes = data.segments.sumOf { it.trans.size }
                                val allLabels = data.segments.flatMap { seg ->
                                    seg.trans.map { it.label }
                                }.toSet()
                                val lyricsPreview = data.segments.firstOrNull()?.lyrics?.take(30) ?: ""
                                transSummary = """
                                    |Trans Data (.trans JSON):
                                    |  Segments: ${data.count}
                                    |  Total notes: $totalNotes
                                    |  Unique svaras: ${allLabels.size}
                                    |  Labels: ${allLabels.joinToString(", ")}
                                    |  First lyrics: "$lyricsPreview..."
                                """.trimMargin()
                                status = "Trans file parsed"
                            } else {
                                status = "Failed to parse trans file"
                            }
                        } catch (e: Exception) {
                            Napier.e("Parse error", e)
                            status = "Error: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Parse .trans")
            }
        }

        // Results display
        pitchSummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        notesSummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        transSummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
