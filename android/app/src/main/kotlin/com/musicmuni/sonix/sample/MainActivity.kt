package com.musicmuni.sonix.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.musicmuni.sonix.Sonix
import com.musicmuni.sonix.sample.components.OptionChip
import com.musicmuni.sonix.sample.simplified.*
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Napier.d("Microphone permission granted")
        } else {
            Napier.w("Microphone permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize logging
        Napier.base(DebugAntilog())

        // Initialize Sonix SDK with API key from BuildConfig
        Sonix.initialize(BuildConfig.SONIX_API_KEY, this)

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SonixSampleApp(context = this)
                }
            }
        }
    }
}

/**
 * API mode toggle: Simple (new Sonix facade) vs Advanced (low-level APIs)
 */
enum class ApiMode {
    SIMPLE,
    ADVANCED
}

@Composable
fun SonixSampleApp(context: ComponentActivity) {
    val scrollState = rememberScrollState()
    var apiMode by remember { mutableStateOf(ApiMode.SIMPLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with API mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sonix Sample App",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // API Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("API Mode:", style = MaterialTheme.typography.bodyMedium)
            OptionChip(
                selected = apiMode == ApiMode.SIMPLE,
                onClick = { apiMode = ApiMode.SIMPLE },
                label = "Simple"
            )
            OptionChip(
                selected = apiMode == ApiMode.ADVANCED,
                onClick = { apiMode = ApiMode.ADVANCED },
                label = "Advanced"
            )
        }

        Text(
            text = if (apiMode == ApiMode.SIMPLE)
                "Using simplified Sonix.* API (recommended for most apps)"
            else
                "Using low-level com.musicmuni.sonix.api.* (for power users)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // Show sections based on API mode - each in a Card for visual separation
        // Use key() to ensure proper cleanup when switching modes
        key(apiMode) {
            when (apiMode) {
                ApiMode.SIMPLE -> {
                    SectionCard { RecordingSectionSimplified(context) }
                    SectionCard { PlaybackSectionSimplified(context) }
                    SectionCard { MultiTrackSectionSimplified(context) }
                    SectionCard { MidiSectionSimplified(context) }
                    SectionCard { MetronomeSectionSimplified(context) }
                }
                ApiMode.ADVANCED -> {
                    SectionCard { RecordingSection(context) }
                    SectionCard { PlaybackSection(context) }
                    SectionCard { MultiTrackSection(context) }
                    SectionCard { MidiSection(context) }
                    SectionCard { MetronomeSection(context) }
                    SectionCard { DecodingSection(context) }
                    SectionCard { ParserSection(context) }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Wrapper card for each section to provide visual separation.
 */
@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
