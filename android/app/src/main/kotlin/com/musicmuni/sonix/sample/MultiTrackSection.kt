package com.musicmuni.sonix.sample

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.sonix.SonixMixer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Advanced Multi-Track Section using SonixMixer.Builder().
 *
 * Demonstrates the Builder pattern for advanced configuration:
 * - Loop count configuration
 * - Playback event callbacks (onPlaybackComplete, onLoopComplete, onError)
 * - Auto-decoding from file paths (no manual AudioDecoder usage needed)
 *
 * Compare this to MultiTrackSectionSimplified.kt which uses SonixMixer.create().
 */
@Composable
fun MultiTrackSection(context: Context) {
    val scope = rememberCoroutineScope()

    var mixer by remember { mutableStateOf<SonixMixer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var backingVolume by remember { mutableFloatStateOf(0.8f) }
    var vocalVolume by remember { mutableFloatStateOf(1.0f) }
    var status by remember { mutableStateOf("Ready") }
    var isLoaded by remember { mutableStateOf(false) }

    // Initialize mixer
    LaunchedEffect(Unit) {
        try {
            status = "Loading tracks..."

            // Copy assets to files
            val (backingPath, vocalPath) = withContext(Dispatchers.IO) {
                val backingFile = copyAssetToFile(context, "sample.m4a")
                val vocalFile = copyAssetToFile(context, "vocal.m4a")
                Pair(backingFile.absolutePath, vocalFile.absolutePath)
            }

            // CREATE MIXER WITH BUILDER - ADVANCED CONFIGURATION
            val newMixer = SonixMixer.Builder()
                .loopCount(1)  // Play once (default); use loopForever() for infinite
                .onPlaybackComplete {
                    Napier.d("Playback complete!")
                    status = "Playback complete"
                }
                .onLoopComplete { loopIndex ->
                    Napier.d("Completed loop $loopIndex")
                }
                .onError { error ->
                    Napier.e("Playback error: $error")
                    status = "Error: $error"
                }
                .build()

            mixer = newMixer

            // Auto-decode tracks (no manual AudioDecoder calls needed!)
            scope.launch {
                val backingSuccess = newMixer.addTrack("backing", backingPath)
                val vocalSuccess = newMixer.addTrack("vocal", vocalPath)

                if (backingSuccess && vocalSuccess) {
                    durationMs = newMixer.duration
                    isLoaded = true
                    status = "Loaded 2 tracks"
                    Napier.d("Multi-track loaded: duration=${newMixer.duration}ms")
                } else {
                    status = "Failed to load tracks"
                }
            }

            // Observe state using StateFlow collection
            launch {
                newMixer.isPlaying.collect { playing ->
                    isPlaying = playing
                }
            }

            launch {
                newMixer.currentTime.collect { time ->
                    currentTimeMs = time
                }
            }

            launch {
                newMixer.error.collect { error ->
                    error?.let {
                        status = "Error: ${it.message}"
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("MultiTrack init failed", e)
            status = "Error: ${e.message}"
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mixer?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Multi-Track (Builder API)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Time display
        Text(
            text = "${formatTimeMs(currentTimeMs)} / ${formatTimeMs(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        // Seek slider
        Slider(
            value = if (durationMs > 0) currentTimeMs.toFloat() / durationMs else 0f,
            onValueChange = { fraction ->
                val seekPos = (fraction * durationMs).toLong()
                mixer?.seek(seekPos)
            },
            enabled = isLoaded,
            modifier = Modifier.fillMaxWidth()
        )

        // Backing volume
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Backing:", modifier = Modifier.width(70.dp))
            Slider(
                value = backingVolume,
                onValueChange = {
                    backingVolume = it
                    mixer?.setTrackVolume("backing", it)
                },
                modifier = Modifier.weight(1f)
            )
            Text("${(backingVolume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Vocal volume
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Vocal:", modifier = Modifier.width(70.dp))
            Slider(
                value = vocalVolume,
                onValueChange = {
                    vocalVolume = it
                    mixer?.setTrackVolume("vocal", it)
                },
                modifier = Modifier.weight(1f)
            )
            Text("${(vocalVolume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mixer?.play() },
                enabled = isLoaded && !isPlaying
            ) {
                Text("Play")
            }
            Button(
                onClick = { mixer?.pause() },
                enabled = isPlaying
            ) {
                Text("Pause")
            }
            Button(
                onClick = {
                    mixer?.stop()
                    currentTimeMs = 0
                },
                enabled = isLoaded
            ) {
                Text("Stop")
            }
        }

        // Fade buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    mixer?.fadeTrackVolume("vocal", 0.2f, 500)
                    vocalVolume = 0.2f
                },
                enabled = isLoaded
            ) {
                Text("Fade Vocal Down")
            }
            Button(
                onClick = {
                    mixer?.fadeTrackVolume("vocal", 1.0f, 500)
                    vocalVolume = 1.0f
                },
                enabled = isLoaded
            ) {
                Text("Fade Vocal Up")
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}
