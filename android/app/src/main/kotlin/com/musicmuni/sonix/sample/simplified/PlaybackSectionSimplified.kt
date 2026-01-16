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
import com.musicmuni.vozos.sonix.SonixPlayer
import com.musicmuni.vozos.sonix.sample.components.OptionChip
import com.musicmuni.vozos.sonix.sample.copyAssetToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simplified Playback Section using the new SonixPlayer API.
 *
 * Demonstrates SonixPlayer.create() - the zero-config factory method.
 */
@Composable
fun PlaybackSectionSimplified(context: Context) {
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    var player by remember { mutableStateOf<SonixPlayer?>(null) }
    var status by remember { mutableStateOf("Loading...") }

    // UI state derived from SonixPlayer StateFlows
    val isPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentTimeMs by player?.currentTime?.collectAsState() ?: remember { mutableStateOf(0L) }
    val durationMs = player?.duration ?: 0L

    // Initialize player - ONE LINE to create and load!
    LaunchedEffect(Unit) {
        try {
            val assetFile = withContext(Dispatchers.IO) {
                copyAssetToFile(context, "sample.m4a")
            }
            // THIS IS ALL IT TAKES TO CREATE A PLAYER!
            player = SonixPlayer.create(assetFile.absolutePath)
            status = "Loaded: sample.m4a"
        } catch (e: Exception) {
            status = "Error: ${e.message}"
            Toast.makeText(localContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Playback (SonixPlayer.create)", style = MaterialTheme.typography.titleMedium)
        Text("Status: $status", style = MaterialTheme.typography.bodySmall)

        // Time display & seek
        Text("${formatTime(currentTimeMs)} / ${formatTime(durationMs)}")
        Slider(
            value = if (durationMs > 0) currentTimeMs.toFloat() / durationMs else 0f,
            onValueChange = { player?.seek((it * durationMs).toLong()) },
            enabled = player != null,
            modifier = Modifier.fillMaxWidth()
        )

        // Playback controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { player?.play() }, enabled = player != null && !isPlaying) { Text("Play") }
            Button(onClick = { player?.pause() }, enabled = isPlaying) { Text("Pause") }
            Button(onClick = { player?.stop() }, enabled = player != null) { Text("Stop") }
        }

        // Volume - property-style access!
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume:", modifier = Modifier.width(60.dp))
            var volume by remember { mutableFloatStateOf(1f) }
            Slider(
                value = volume,
                onValueChange = { volume = it; player?.let { p -> p.volume = it } },
                modifier = Modifier.weight(1f)
            )
            Text("${(volume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Pitch - only apply on drag end for responsiveness
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pitch:", modifier = Modifier.width(60.dp))
            var pitch by remember { mutableFloatStateOf(0f) }
            Slider(
                value = (pitch + 12f) / 24f,
                onValueChange = { pitch = (it * 24f) - 12f },
                onValueChangeFinished = { player?.let { p -> p.pitch = pitch } },
                modifier = Modifier.weight(1f)
            )
            Text("${pitch.toInt()} st", modifier = Modifier.width(40.dp))
        }

        // Loop count - property-style access!
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Loop:", modifier = Modifier.width(60.dp))
            var loopCount by remember { mutableIntStateOf(1) }
            listOf(1, 2, 3, -1).forEach { count ->
                OptionChip(
                    selected = loopCount == count,
                    onClick = { loopCount = count; player?.let { p -> p.loopCount = count } },
                    label = if (count == -1) "∞" else count.toString()
                )
            }
        }

        // Fade controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { scope.launch { player?.fadeIn(1.0f, 1000) } },
                enabled = player != null
            ) { Text("Fade In") }
            Button(
                onClick = { scope.launch { player?.fadeOut(1000) } },
                enabled = player != null
            ) { Text("Fade Out") }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}
