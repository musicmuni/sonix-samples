package com.musicmuni.sonix.sample

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.sonix.Sonix
import com.musicmuni.sonix.SonixPlayer
import com.musicmuni.sonix.SonixRecorder
import com.musicmuni.sonix.sample.components.OptionChip
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import java.io.File

enum class OutputFormat(val extension: String, val displayName: String) {
    M4A("m4a", "M4A (AAC)"),
    MP3("mp3", "MP3 (LAME)")
}

/**
 * Recording Section using the unified SonixRecorder Builder API.
 *
 * This demonstrates advanced usage with SonixRecorder.Builder() including:
 * - Custom sample rate, channels, and bitrate
 * - Buffer Pool status monitoring (for Calibra integration)
 * - SonixPlayer for playback after recording
 */
@Composable
fun RecordingSection(context: Context) {
    val scope = rememberCoroutineScope()

    var recorder by remember { mutableStateOf<SonixRecorder?>(null) }
    var savedFilePath by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Ready") }
    var selectedFormat by remember { mutableStateOf(OutputFormat.M4A) }

    // UI state derived from SonixRecorder StateFlows
    val isRecording by recorder?.isRecording?.collectAsState() ?: remember { mutableStateOf(false) }
    val durationMs by recorder?.duration?.collectAsState() ?: remember { mutableStateOf(0L) }
    val audioLevel by recorder?.level?.collectAsState() ?: remember { mutableStateOf(0f) }
    val error by recorder?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    // Buffer Pool metrics (for Calibra integration / zero-alloc monitoring)
    val bufferPoolAvailable = recorder?.bufferPoolAvailable ?: 4
    val bufferPoolWasExhausted = recorder?.bufferPoolWasExhausted ?: false

    // Playback state
    var player by remember { mutableStateOf<SonixPlayer?>(null) }
    val isPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val playbackTimeMs by player?.currentTime?.collectAsState() ?: remember { mutableStateOf(0L) }
    var playbackDurationMs by remember { mutableLongStateOf(0L) }

    // Update status on error
    LaunchedEffect(error) {
        error?.let { status = "Error: ${it.message}" }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎙️ Recording",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Format selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Format:", style = MaterialTheme.typography.bodySmall)
            OutputFormat.entries.forEach { format ->
                OptionChip(
                    selected = selectedFormat == format,
                    onClick = {
                        if (!isRecording) {
                            selectedFormat = format
                        }
                    },
                    label = format.displayName,
                    enabled = !isRecording
                )
            }
        }

        // Duration display
        Text(
            text = "Duration: ${formatDuration(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        // Audio level indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Level:", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { audioLevel.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
            )
        }

        // Buffer pool status (demonstrates zero-allocation processing for Calibra integration)
        if (isRecording) {
            Text(
                text = "Buffer Pool: $bufferPoolAvailable/4 available" +
                        if (bufferPoolWasExhausted) " (overflow occurred)" else " (zero-alloc)",
                style = MaterialTheme.typography.bodySmall,
                color = if (bufferPoolWasExhausted) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
        }

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val outputDir = context.filesDir.absolutePath
                    val timestamp = System.currentTimeMillis()
                    val outputPath = "$outputDir/recording_$timestamp.${selectedFormat.extension}"

                    // Create recorder using Builder for advanced config with callbacks
                    val newRecorder = SonixRecorder.Builder()
                        .outputPath(outputPath)
                        .format(selectedFormat.extension)
                        .sampleRate(16000)
                        .channels(1)
                        .bitrate(128000)
                        .onRecordingStarted {
                            Napier.d("Recording started!")
                        }
                        .onRecordingStopped { path ->
                            Napier.d("Recording saved to: $path")
                        }
                        .onError { error ->
                            Napier.e("Recording error: $error")
                        }
                        .build()

                    recorder = newRecorder
                    savedFilePath = outputPath
                    newRecorder.start()
                    status = "Recording (${selectedFormat.displayName})"
                },
                enabled = !isRecording
            ) {
                Text("Record")
            }

            Button(
                onClick = {
                    recorder?.stop()
                    status = "Saved ${selectedFormat.displayName}"
                },
                enabled = isRecording
            ) {
                Text("Stop & Save")
            }
        }

        // Show saved file info and playback controls
        savedFilePath?.let { path ->
            val file = File(path)
            if (file.exists() && !isRecording) {
                Text(
                    text = "Saved: ${file.name} (${file.length() / 1024} KB)",
                    style = MaterialTheme.typography.bodySmall
                )

                // Playback time display
                if (playbackDurationMs > 0) {
                    Text(
                        text = "Playback: ${formatDuration(playbackTimeMs)} / ${formatDuration(playbackDurationMs)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Playback controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                // Release old player if exists
                                player?.release()
                                player = null

                                // Create player using unified SonixPlayer API
                                val newPlayer = SonixPlayer.create(path)
                                player = newPlayer
                                playbackDurationMs = newPlayer.duration

                                newPlayer.play()
                                status = "Playing recording"
                            }
                        },
                        enabled = !isRecording && !isPlaying
                    ) {
                        Text("Play")
                    }

                    Button(
                        onClick = {
                            player?.pause()
                            status = "Paused"
                        },
                        enabled = isPlaying
                    ) {
                        Text("Pause")
                    }

                    Button(
                        onClick = {
                            player?.stop()
                            status = "Stopped"
                        },
                        enabled = player != null
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}
