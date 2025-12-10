package com.musicmuni.sonix.sample.simplified

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musicmuni.sonix.SonixRecorder
import com.musicmuni.sonix.sample.components.OptionChip
import java.io.File

/**
 * Simplified Recording Section using the new Sonix API.
 *
 * Compare this to RecordingSection.kt (398 lines) - this version is ~80 lines
 * because all the audio complexity is handled internally by the Sonix facade.
 */
@Composable
fun RecordingSectionSimplified(context: Context) {
    val localContext = LocalContext.current
    var recording by remember { mutableStateOf<SonixRecorder?>(null) }
    var selectedFormat by remember { mutableStateOf("m4a") }
    var savedFilePath by remember { mutableStateOf<String?>(null) }

    // UI state derived from Recording StateFlows
    val isRecording by recording?.isRecording?.collectAsState() ?: remember { mutableStateOf(false) }
    val duration by recording?.duration?.collectAsState() ?: remember { mutableStateOf(0L) }
    val level by recording?.level?.collectAsState() ?: remember { mutableStateOf(0f) }
    val error by recording?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    // Show toast on error
    LaunchedEffect(error) {
        error?.let { err ->
            Toast.makeText(localContext, "Error: ${err.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { recording?.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Recording (Simplified API)", style = MaterialTheme.typography.titleMedium)

        // Format selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Format:", style = MaterialTheme.typography.bodySmall)
            listOf("m4a", "mp3").forEach { format ->
                OptionChip(
                    selected = selectedFormat == format,
                    onClick = { if (!isRecording) selectedFormat = format },
                    label = format.uppercase(),
                    enabled = !isRecording
                )
            }
        }

        // Duration & level
        Text("Duration: ${formatDuration(duration)}", style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Level:", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { level.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(8.dp)
            )
        }

        // Control buttons - THE SIMPLE PART!
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val outputPath = "${context.filesDir}/recording_${System.currentTimeMillis()}.$selectedFormat"

                    // THIS IS ALL IT TAKES TO RECORD!
                    val newRecording = SonixRecorder.create(
                        outputPath = outputPath,
                        format = selectedFormat,
                        quality = "voice"
                    )
                    recording = newRecording
                    savedFilePath = outputPath
                    newRecording.start()
                },
                enabled = !isRecording
            ) { Text("Record") }

            Button(
                onClick = { recording?.stop() },
                enabled = isRecording
            ) { Text("Stop & Save") }
        }

        // Show saved file
        savedFilePath?.let { path ->
            val file = File(path)
            if (file.exists() && !isRecording) {
                Text(
                    "Saved: ${file.name} (${file.length() / 1024} KB)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}
