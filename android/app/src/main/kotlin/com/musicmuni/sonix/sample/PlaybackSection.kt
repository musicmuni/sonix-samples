package com.musicmuni.sonix.sample

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.vozos.sonix.SonixPlayer
import com.musicmuni.vozos.sonix.sample.components.OptionChip
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PlaybackSection(context: Context) {
    val scope = rememberCoroutineScope()

    var player by remember { mutableStateOf<SonixPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var loopCount by remember { mutableIntStateOf(1) }
    var status by remember { mutableStateOf("Ready") }
    var isLoaded by remember { mutableStateOf(false) }

    // Initialize player
    LaunchedEffect(Unit) {
        try {
            status = "Loading..."

            // Copy asset to file on IO dispatcher
            val assetFile = withContext(Dispatchers.IO) {
                copyAssetToFile(context, "sample.m4a")
            }

            // Create player using Builder pattern with callbacks
            val newPlayer = SonixPlayer.Builder()
                .source(assetFile.absolutePath)
                .volume(volume)
                .pitch(pitch)
                .loopCount(loopCount)
                .onComplete {
                    Napier.d("Playback completed!")
                }
                .onLoopComplete { loopIndex, totalLoops ->
                    Napier.d("Loop $loopIndex of $totalLoops completed")
                }
                .onError { error ->
                    Napier.e("Playback error: $error")
                    status = "Error: $error"
                }
                .build()

            player = newPlayer
            durationMs = newPlayer.duration
            isLoaded = true
            status = "Loaded: sample.m4a"
            Napier.d("Audio loaded, duration: $durationMs ms")

            // Observe playback state
            launch {
                newPlayer.isPlaying.collect { playing ->
                    isPlaying = playing
                }
            }

            launch {
                newPlayer.currentTime.collect { time ->
                    currentTimeMs = time
                }
            }
        } catch (e: Exception) {
            Napier.e("Player init failed", e)
            status = "Error: ${e.message}"
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Playback (SonixPlayer.Builder)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Time display
        Text(
            text = "${formatTime(currentTimeMs)} / ${formatTime(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        // Seek slider
        Slider(
            value = if (durationMs > 0) currentTimeMs.toFloat() / durationMs else 0f,
            onValueChange = { fraction ->
                val seekPos = (fraction * durationMs).toLong()
                player?.seek(seekPos)
            },
            enabled = isLoaded,
            modifier = Modifier.fillMaxWidth()
        )

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { player?.play() },
                enabled = isLoaded && !isPlaying
            ) {
                Text("Play")
            }
            Button(
                onClick = { player?.pause() },
                enabled = isPlaying
            ) {
                Text("Pause")
            }
            Button(
                onClick = {
                    player?.stop()
                    currentTimeMs = 0
                },
                enabled = isLoaded
            ) {
                Text("Stop")
            }
        }

        // Volume control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Volume:", modifier = Modifier.width(60.dp))
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    player?.let { p -> p.volume = it }
                },
                modifier = Modifier.weight(1f)
            )
            Text("${(volume * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }

        // Pitch control - realtime updates
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Pitch:", modifier = Modifier.width(60.dp))
            Slider(
                value = (pitch + 12f) / 24f,
                onValueChange = {
                    pitch = (it * 24f) - 12f
                    player?.let { p -> p.pitch = pitch }
                },
                modifier = Modifier.weight(1f)
            )
            Text("${pitch.toInt()} st", modifier = Modifier.width(40.dp))
        }

        // Loop count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Loop:", modifier = Modifier.width(60.dp))
            listOf(1, 2, 3, -1).forEach { count ->
                OptionChip(
                    selected = loopCount == count,
                    onClick = {
                        loopCount = count
                        player?.let { p -> p.loopCount = count }
                    },
                    label = if (count == -1) "∞" else count.toString()
                )
            }
        }

        // Fade controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        player?.fadeIn(1.0f, 1000)
                    }
                },
                enabled = isLoaded
            ) {
                Text("Fade In")
            }
            Button(
                onClick = {
                    scope.launch {
                        player?.fadeOut(1000)
                    }
                },
                enabled = isLoaded
            ) {
                Text("Fade Out")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}

fun copyAssetToFile(context: Context, assetName: String): File {
    val file = File(context.cacheDir, assetName)
    if (!file.exists()) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }
    return file
}
