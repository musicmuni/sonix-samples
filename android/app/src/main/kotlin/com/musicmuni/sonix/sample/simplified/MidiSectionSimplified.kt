package com.musicmuni.sonix.sample.simplified

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.sonix.SonixMidiSynthesizer
import com.musicmuni.sonix.SonixPlayer
import com.musicmuni.sonix.midi.MidiNote
import com.musicmuni.sonix.sample.copyAssetToFile
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Simplified MIDI Synthesis Section using SonixMidiSynthesizer.
 *
 * Demonstrates SonixMidiSynthesizer.create() - the zero-config factory method.
 */
@Composable
fun MidiSectionSimplified(context: Context) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<SonixPlayer?>(null) }
    var synthesizerVersion by remember { mutableStateOf<String?>(null) }

    // Observe player state
    val isPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

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
            text = "MIDI Synthesis (SonixMidiSynthesizer.create)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Show FluidSynth version if available
        synthesizerVersion?.let {
            Text(text = "FluidSynth: $it", style = MaterialTheme.typography.bodySmall)
        }

        // C major scale notes
        Text(
            text = "Notes: C4 - D4 - E4 - F4 - G4 - A4 - B4 - C5",
            style = MaterialTheme.typography.bodySmall
        )

        // Synthesize button
        Button(
            onClick = {
                scope.launch {
                    isSynthesizing = true
                    status = "Synthesizing..."

                    try {
                        // Copy soundfont to file
                        val sfFile = copyAssetToFile(context, "harmonium.sf2")

                        // C major scale
                        val notes = listOf(
                            MidiNote(note = 60, startTime = 0f, endTime = 400f),     // C4
                            MidiNote(note = 62, startTime = 500f, endTime = 900f),   // D4
                            MidiNote(note = 64, startTime = 1000f, endTime = 1400f), // E4
                            MidiNote(note = 65, startTime = 1500f, endTime = 1900f), // F4
                            MidiNote(note = 67, startTime = 2000f, endTime = 2400f), // G4
                            MidiNote(note = 69, startTime = 2500f, endTime = 2900f), // A4
                            MidiNote(note = 71, startTime = 3000f, endTime = 3400f), // B4
                            MidiNote(note = 72, startTime = 3500f, endTime = 4400f), // C5
                        )

                        val outFile = File(context.cacheDir, "midi_output.wav")

                        val (success, version) = withContext(Dispatchers.IO) {
                            // SIMPLE API: SonixMidiSynthesizer.create()
                            val synth = SonixMidiSynthesizer.create(
                                soundFontPath = sfFile.absolutePath
                            )
                            Napier.d("MIDI Synth version: ${synth.version}")

                            val result = synth.synthesizeFromNotes(
                                notes = notes,
                                outputPath = outFile.absolutePath
                            )
                            Pair(result, synth.version)
                        }

                        // Check if still active before updating UI state
                        if (!isActive) return@launch

                        synthesizerVersion = version
                        if (success && outFile.exists() && outFile.length() > 0) {
                            outputFile = outFile
                            status = "Generated: ${outFile.length() / 1024} KB"

                            // Load into SonixPlayer for playback
                            player?.release()
                            val newPlayer = SonixPlayer.create(outFile.absolutePath)
                            player = newPlayer
                        } else {
                            status = "Synthesis failed"
                        }
                    } catch (e: Exception) {
                        Napier.e("MIDI synthesis failed", e)
                        if (isActive) status = "Error: ${e.message}"
                    } finally {
                        if (isActive) isSynthesizing = false
                    }
                }
            },
            enabled = !isSynthesizing
        ) {
            Text(if (isSynthesizing) "Synthesizing..." else "Synthesize")
        }

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { player?.play() },
                enabled = outputFile != null && !isSynthesizing && !isPlaying
            ) {
                Text("Play")
            }

            Button(
                onClick = { player?.stop() },
                enabled = isPlaying
            ) {
                Text("Stop")
            }
        }
    }
}
