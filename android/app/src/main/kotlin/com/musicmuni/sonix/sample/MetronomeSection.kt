package com.musicmuni.sonix.sample

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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Advanced Metronome Section using SonixMetronome.Builder().
 *
 * Demonstrates the Builder pattern for advanced configuration:
 * - Beat callback for UI synchronization
 * - Error callback for handling failures
 * - Volume and BPM configuration
 *
 * Compare this to MetronomeSectionSimplified.kt which uses SonixMetronome.create().
 */
@Composable
fun MetronomeSection(context: Context) {
    val scope = rememberCoroutineScope()

    var metronome by remember { mutableStateOf<SonixMetronome?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var beatsPerCycle by remember { mutableIntStateOf(4) }
    var currentBeat by remember { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var status by remember { mutableStateOf("Ready") }
    var isInitialized by remember { mutableStateOf(false) }

    // Store paths for reinitialization
    var samaSamplePath by remember { mutableStateOf<String?>(null) }
    var beatSamplePath by remember { mutableStateOf<String?>(null) }

    // Initialize metronome
    LaunchedEffect(Unit) {
        try {
            status = "Initializing..."

            // Copy files on IO dispatcher
            val (samaPath, beatPath) = withContext(Dispatchers.IO) {
                val samaFile = copyAssetToFile(context, "sama_click.wav")
                val beatFile = copyAssetToFile(context, "beat_click.wav")
                Pair(samaFile.absolutePath, beatFile.absolutePath)
            }

            // Store paths for reinitialization
            samaSamplePath = samaPath
            beatSamplePath = beatPath

            // CREATE METRONOME WITH BUILDER - ADVANCED CONFIGURATION
            val newMetronome = SonixMetronome.Builder()
                .samaSamplePath(samaPath)
                .beatSamplePath(beatPath)
                .bpm(bpm)
                .beatsPerCycle(beatsPerCycle)
                .volume(volume)
                .onBeat { beat ->
                    // This callback fires on each beat - useful for haptics, LEDs, etc.
                    Napier.d("Beat $beat")
                }
                .onError { error ->
                    Napier.e("Metronome error: $error")
                    status = "Error: $error"
                }
                .build()

            metronome = newMetronome

            // Observe state using StateFlow collection
            launch {
                newMetronome.currentBeat.collect { beat ->
                    currentBeat = beat
                }
            }

            launch {
                newMetronome.isPlaying.collect { playing ->
                    isRunning = playing
                }
            }

            launch {
                newMetronome.isInitialized.collect { initialized ->
                    isInitialized = initialized
                    if (initialized) {
                        status = "Initialized"
                    }
                }
            }

            launch {
                newMetronome.error.collect { error ->
                    error?.let {
                        status = "Error: ${it.message}"
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("Metronome init failed", e)
            status = "Error: ${e.message}"
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            metronome?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Metronome (Builder API)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // BPM slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("BPM:", modifier = Modifier.width(40.dp))
            Slider(
                value = (bpm - 60f) / 140f,  // 60-200 range
                onValueChange = {
                    bpm = 60f + (it * 140f)
                    metronome?.setBpm(bpm)
                },
                modifier = Modifier.weight(1f)
            )
            Text("${bpm.toInt()}", modifier = Modifier.width(40.dp))
        }

        // Beats per cycle (display only - set during initialization)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Beats:", modifier = Modifier.width(50.dp))
            listOf(3, 4, 6, 8).forEach { count ->
                OptionChip(
                    selected = beatsPerCycle == count,
                    onClick = {
                        if (beatsPerCycle != count) {
                            beatsPerCycle = count
                            // Reinitialize metronome with new beatsPerCycle
                            val samaPath = samaSamplePath
                            val beatPath = beatSamplePath
                            if (samaPath != null && beatPath != null) {
                                scope.launch {
                                    try {
                                        metronome?.release()
                                        val newMetronome = SonixMetronome.Builder()
                                            .samaSamplePath(samaPath)
                                            .beatSamplePath(beatPath)
                                            .bpm(bpm)
                                            .beatsPerCycle(count)
                                            .volume(volume)
                                            .onBeat { beat ->
                                                Napier.d("Beat $beat")
                                            }
                                            .onError { error ->
                                                status = "Error: $error"
                                            }
                                            .build()

                                        metronome = newMetronome
                                        currentBeat = 0
                                        status = "Beats: $count"

                                        // Re-observe state
                                        launch {
                                            newMetronome.currentBeat.collect { beat ->
                                                currentBeat = beat
                                            }
                                        }
                                        launch {
                                            newMetronome.isPlaying.collect { playing ->
                                                isRunning = playing
                                            }
                                        }
                                        launch {
                                            newMetronome.isInitialized.collect { initialized ->
                                                isInitialized = initialized
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Napier.e("Metronome reinit failed", e)
                                        status = "Error: ${e.message}"
                                    }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Vol:", modifier = Modifier.width(40.dp))
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    metronome?.let { m -> m.volume = it }
                },
                modifier = Modifier.weight(1f)
            )
            Text("${(volume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Beat indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    metronome?.start()
                    status = "Running"
                },
                enabled = isInitialized && !isRunning
            ) {
                Text("Start")
            }
            Button(
                onClick = {
                    metronome?.stop()
                    status = "Stopped"
                    currentBeat = 0
                },
                enabled = isRunning
            ) {
                Text("Stop")
            }
        }
    }
}
