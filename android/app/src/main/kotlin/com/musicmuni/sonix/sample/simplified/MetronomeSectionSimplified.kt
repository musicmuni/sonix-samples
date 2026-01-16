package com.musicmuni.sonix.sample.simplified

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musicmuni.vozos.sonix.SonixMetronome
import com.musicmuni.vozos.sonix.sample.components.OptionChip
import com.musicmuni.vozos.sonix.sample.copyAssetToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simplified Metronome Section using SonixMetronome.create().
 *
 * Demonstrates the zero-config factory pattern for metronome playback.
 * Compare this to MetronomeSection.kt which uses SonixMetronome.Builder().
 */
@Composable
fun MetronomeSectionSimplified(context: Context) {
    val scope = rememberCoroutineScope()

    var metronome by remember { mutableStateOf<SonixMetronome?>(null) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var beatsPerCycle by remember { mutableIntStateOf(4) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var status by remember { mutableStateOf("Initializing...") }

    // Paths for reinitialization
    var samaSamplePath by remember { mutableStateOf<String?>(null) }
    var beatSamplePath by remember { mutableStateOf<String?>(null) }

    // UI state from SonixMetronome StateFlows
    val currentBeat by metronome?.currentBeat?.collectAsState() ?: remember { mutableStateOf(0) }
    val isRunning by metronome?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val isInitialized by metronome?.isInitialized?.collectAsState() ?: remember { mutableStateOf(false) }

    // Update status when initialized
    LaunchedEffect(isInitialized) {
        if (isInitialized) {
            status = "Ready"
        }
    }

    // Initialize metronome
    LaunchedEffect(Unit) {
        val (samaPath, beatPath) = withContext(Dispatchers.IO) {
            val sama = copyAssetToFile(context, "sama_click.wav")
            val beat = copyAssetToFile(context, "beat_click.wav")
            Pair(sama.absolutePath, beat.absolutePath)
        }
        samaSamplePath = samaPath
        beatSamplePath = beatPath

        // CREATE METRONOME - ZERO-CONFIG FACTORY!
        metronome = SonixMetronome.create(
            samaSamplePath = samaPath,
            beatSamplePath = beatPath,
            bpm = bpm,
            beatsPerCycle = beatsPerCycle
        )
        // Note: status will update to "Ready" via the isInitialized LaunchedEffect
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { metronome?.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Metronome (Simplified API)", style = MaterialTheme.typography.titleMedium)
        Text("Status: $status", style = MaterialTheme.typography.bodySmall)

        // BPM slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BPM:", modifier = Modifier.width(40.dp))
            Slider(
                value = (bpm - 60f) / 140f,
                onValueChange = { bpm = 60f + (it * 140f); metronome?.setBpm(bpm) },
                modifier = Modifier.weight(1f)
            )
            Text("${bpm.toInt()}", modifier = Modifier.width(40.dp))
        }

        // Beats per cycle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Beats:", modifier = Modifier.width(50.dp))
            listOf(3, 4, 6, 8).forEach { count ->
                OptionChip(
                    selected = beatsPerCycle == count,
                    onClick = {
                        if (!isRunning && beatsPerCycle != count) {
                            beatsPerCycle = count
                            // Reinitialize with new beat count
                            val samaPath = samaSamplePath
                            val beatPath = beatSamplePath
                            if (samaPath != null && beatPath != null) {
                                scope.launch {
                                    metronome?.release()
                                    metronome = SonixMetronome.create(
                                        samaSamplePath = samaPath,
                                        beatSamplePath = beatPath,
                                        bpm = bpm,
                                        beatsPerCycle = count
                                    )
                                    status = "Beats: $count"
                                }
                            }
                        }
                    },
                    label = count.toString(),
                    enabled = !isRunning
                )
            }
        }

        // Volume
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vol:", modifier = Modifier.width(40.dp))
            Slider(
                value = volume,
                onValueChange = { volume = it; metronome?.let { m -> m.volume = it } },
                modifier = Modifier.weight(1f)
            )
            Text("${(volume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Beat indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Beat:", modifier = Modifier.width(50.dp))
            (0 until beatsPerCycle).forEach { beat ->
                val isCurrentBeat = beat == currentBeat && isRunning
                val isSama = beat == 0
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrentBeat && isSama -> Color.Red
                                isCurrentBeat -> Color.Green
                                isSama -> Color.Red.copy(alpha = 0.3f)
                                else -> Color.Gray.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        // Control buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { metronome?.start(); status = "Running" },
                enabled = isInitialized && !isRunning
            ) { Text("Start") }
            Button(
                onClick = { metronome?.stop(); status = "Stopped" },
                enabled = isRunning
            ) { Text("Stop") }
        }
    }
}
