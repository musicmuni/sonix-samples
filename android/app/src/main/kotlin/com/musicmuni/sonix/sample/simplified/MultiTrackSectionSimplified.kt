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
import com.musicmuni.vozos.sonix.SonixMixer
import com.musicmuni.vozos.sonix.sample.copyAssetToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simplified Multi-Track Section using SonixMixer.create().
 *
 * Demonstrates the zero-config factory pattern for multi-track playback.
 * Compare this to MultiTrackSection.kt which uses SonixMixer.Builder().
 */
@Composable
fun MultiTrackSectionSimplified(context: Context) {
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    var mixer by remember { mutableStateOf<SonixMixer?>(null) }
    var status by remember { mutableStateOf("Loading...") }
    var backingVolume by remember { mutableFloatStateOf(0.8f) }
    var vocalVolume by remember { mutableFloatStateOf(1.0f) }

    // UI state derived from SonixMixer StateFlows
    val isPlaying by mixer?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentTimeMs by mixer?.currentTime?.collectAsState() ?: remember { mutableStateOf(0L) }
    val error by mixer?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    val durationMs = mixer?.duration ?: 0L

    // Show toast on error
    LaunchedEffect(error) {
        error?.let { err ->
            Toast.makeText(localContext, "Error: ${err.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Initialize mixer
    LaunchedEffect(Unit) {
        // Copy assets to files
        val (backingPath, vocalPath) = withContext(Dispatchers.IO) {
            val backing = copyAssetToFile(context, "sample.m4a")
            val vocal = copyAssetToFile(context, "vocal.m4a")
            Pair(backing.absolutePath, vocal.absolutePath)
        }

        // CREATE MIXER - ZERO-CONFIG FACTORY!
        val newMixer = SonixMixer.create()
        mixer = newMixer

        // Just pass file paths - no manual decoding needed!
        // Wait for both tracks to load before updating status
        val backingSuccess = newMixer.addTrack("backing", backingPath)
        val vocalSuccess = newMixer.addTrack("vocal", vocalPath)

        if (backingSuccess && vocalSuccess) {
            status = "Loaded 2 tracks"
        } else {
            status = "Failed to load tracks"
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { mixer?.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Multi-Track (Simplified API)", style = MaterialTheme.typography.titleMedium)
        Text("Status: $status", style = MaterialTheme.typography.bodySmall)

        // Time display & seek
        Text("${formatTimeMs(currentTimeMs)} / ${formatTimeMs(durationMs)}")
        Slider(
            value = if (durationMs > 0) currentTimeMs.toFloat() / durationMs else 0f,
            onValueChange = { mixer?.seek((it * durationMs).toLong()) },
            enabled = mixer != null,
            modifier = Modifier.fillMaxWidth()
        )

        // Per-track volume controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Backing:", modifier = Modifier.width(70.dp))
            Slider(
                value = backingVolume,
                onValueChange = { backingVolume = it; mixer?.setTrackVolume("backing", it) },
                modifier = Modifier.weight(1f)
            )
            Text("${(backingVolume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vocal:", modifier = Modifier.width(70.dp))
            Slider(
                value = vocalVolume,
                onValueChange = { vocalVolume = it; mixer?.setTrackVolume("vocal", it) },
                modifier = Modifier.weight(1f)
            )
            Text("${(vocalVolume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Playback controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { mixer?.play() }, enabled = mixer != null && !isPlaying) { Text("▶") }
            Button(onClick = { mixer?.pause() }, enabled = isPlaying) { Text("⏸") }
            Button(onClick = { mixer?.stop() }, enabled = mixer != null) { Text("⏹") }
        }

        // Fade buttons for vocal track
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { mixer?.fadeTrackVolume("vocal", 0.2f, 500); vocalVolume = 0.2f },
                enabled = mixer != null
            ) { Text("Fade Vocal ↓") }
            Button(
                onClick = { mixer?.fadeTrackVolume("vocal", 1.0f, 500); vocalVolume = 1.0f },
                enabled = mixer != null
            ) { Text("Fade Vocal ↑") }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}
