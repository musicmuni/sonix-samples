package com.musicmuni.sonix.sample

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicmuni.sonix.SonixDecoder
import com.musicmuni.sonix.SonixEncoder
import com.musicmuni.sonix.model.AudioRawData
import com.musicmuni.sonix.sample.components.OptionChip
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Audio Decoding & Encoding Section using SonixDecoder and SonixEncoder.
 *
 * Demonstrates:
 * - SonixDecoder.decode() for extracting raw PCM from audio files
 * - SonixEncoder.encode() for encoding raw PCM to compressed formats
 * - SonixEncoder.isFormatAvailable() for checking platform support
 * - Full decode -> re-encode pipeline (format conversion)
 */
@Composable
fun DecodingSection(context: Context) {
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Ready") }
    var isProcessing by remember { mutableStateOf(false) }
    var decodedInfo by remember { mutableStateOf<DecodedAudioInfo?>(null) }
    var decodedData by remember { mutableStateOf<AudioRawData?>(null) }
    var encodedFile by remember { mutableStateOf<File?>(null) }
    var selectedEncodeFormat by remember { mutableStateOf("m4a") }

    // Check format availability
    val mp3Available = remember { SonixEncoder.isFormatAvailable("mp3") }
    val m4aAvailable = remember { SonixEncoder.isFormatAvailable("m4a") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Audio Decode/Encode (SonixDecoder + SonixEncoder)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)

        // Format availability info
        Text(
            text = "Formats: M4A ${if (m4aAvailable) "✓" else "✗"} | MP3 ${if (mp3Available) "✓" else "✗"}",
            style = MaterialTheme.typography.bodySmall
        )

        // Decode button
        Button(
            onClick = {
                scope.launch {
                    isProcessing = true
                    status = "Decoding sample.m4a..."

                    try {
                        val result = withContext(Dispatchers.IO) {
                            val audioFile = copyAssetToFile(context, "sample.m4a")
                            SonixDecoder.decode(audioFile.absolutePath)
                        }

                        if (result != null) {
                            decodedData = result
                            decodedInfo = DecodedAudioInfo(
                                sampleRate = result.sampleRate,
                                channels = result.numChannels,
                                durationMs = result.durationMilliSecs.toLong(),
                                dataSize = result.audioData.size
                            )
                            status = "Decoded successfully"
                        } else {
                            status = "Decoding failed"
                        }
                    } catch (e: Exception) {
                        Napier.e("Decoding failed", e)
                        status = "Error: ${e.message}"
                    } finally {
                        isProcessing = false
                    }
                }
            },
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Processing..." else "Decode sample.m4a")
        }

        // Show decoded info
        decodedInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Decoded Audio Info", style = MaterialTheme.typography.titleSmall)
                    InfoRow("Sample Rate", "${info.sampleRate} Hz")
                    InfoRow("Channels", info.channels.toString())
                    InfoRow("Duration", "${info.durationMs} ms")
                    InfoRow("PCM Size", "${info.dataSize / 1024} KB (${info.dataSize} bytes)")
                }
            }
        }

        // Encoding section - only show if we have decoded data
        decodedData?.let { data ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Re-encode to:", style = MaterialTheme.typography.titleSmall)

            // Format selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("m4a", "mp3").forEach { format ->
                    val available = if (format == "mp3") mp3Available else m4aAvailable
                    OptionChip(
                        selected = selectedEncodeFormat == format,
                        onClick = { selectedEncodeFormat = format },
                        label = format.uppercase(),
                        enabled = available && !isProcessing
                    )
                }
            }

            // Bitrate options
            var selectedBitrate by remember { mutableIntStateOf(128) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Bitrate:", style = MaterialTheme.typography.bodySmall)
                listOf(64, 128, 192, 256).forEach { bitrate ->
                    OptionChip(
                        selected = selectedBitrate == bitrate,
                        onClick = { selectedBitrate = bitrate },
                        label = "${bitrate}k",
                        enabled = !isProcessing
                    )
                }
            }

            // Encode button
            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        status = "Encoding to $selectedEncodeFormat..."

                        try {
                            val outputPath = "${context.cacheDir}/encoded_output.$selectedEncodeFormat"

                            val success = withContext(Dispatchers.IO) {
                                SonixEncoder.encode(
                                    data = data,
                                    outputPath = outputPath,
                                    format = selectedEncodeFormat,
                                    bitrateKbps = selectedBitrate
                                )
                            }

                            if (success) {
                                val file = File(outputPath)
                                encodedFile = file
                                status = "Encoded: ${file.name} (${file.length() / 1024} KB)"
                            } else {
                                status = "Encoding failed"
                            }
                        } catch (e: Exception) {
                            Napier.e("Encoding failed", e)
                            status = "Error: ${e.message}"
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                enabled = !isProcessing && SonixEncoder.isFormatAvailable(selectedEncodeFormat)
            ) {
                Text("Encode to ${selectedEncodeFormat.uppercase()}")
            }

            // Show encoded file info
            encodedFile?.let { file ->
                if (file.exists()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Encoded File", style = MaterialTheme.typography.titleSmall)
                            InfoRow("File", file.name)
                            InfoRow("Size", "${file.length() / 1024} KB")
                            InfoRow("Compression", "${(decodedInfo?.dataSize?.div(file.length().toInt()) ?: 0)}x")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class DecodedAudioInfo(
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
    val dataSize: Int
)
