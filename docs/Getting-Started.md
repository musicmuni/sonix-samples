# Getting Started with Sonix

Quick start guide to get up and running with Sonix in minutes.

---

## Table of Contents

1. [Installation](#installation)
2. [Initialization](#initialization)
3. [Permissions](#permissions)
4. [First Recording](#first-recording)
5. [First Playback](#first-playback)
6. [Next Steps](#next-steps)

---

## Installation

### Android (AAR)

1. **Add the AAR to your project:**

   Copy `sonix.aar` to `app/libs/` folder.

2. **Add dependencies to `build.gradle.kts`:**

```kotlin
dependencies {
    // Sonix library
    implementation(files("libs/sonix.aar"))

    // Required dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlinx:atomicfu:0.29.0")
    implementation("io.github.aakira:napier:2.7.1")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-android:2.3.12")
}
```

3. **Sync Gradle** and rebuild your project.

### iOS (XCFramework)

1. **Add the XCFramework to Xcode:**

   - Drag `sonix.xcframework` into your Xcode project
   - Ensure it's added to your target's **"Frameworks, Libraries, and Embedded Content"**
   - Set to **"Embed & Sign"**

2. **Import in Swift:**

```swift
import sonix
```

3. **Build your project** to verify installation.

---

## Initialization

**Initialize Sonix with your API key before using any features.**

### Android

In your `Application` class or `MainActivity.onCreate()`:

```kotlin
import com.musicmuni.sonix.Sonix

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Sonix SDK (REQUIRED)
        Sonix.initialize("sk_live_your_api_key_here", this)

        // Optional: Enable debug logging
        Sonix.initializeLogging()

        // Your app code...
    }
}
```

### iOS

In your `App` struct:

```swift
import SwiftUI
import sonix

@main
struct YourApp: App {
    init() {
        // Initialize Sonix SDK (REQUIRED)
        Sonix.initialize(apiKey: "sk_live_your_api_key_here")

        // Optional: Enable debug logging
        Sonix.initializeLogging()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### API Keys

- **Production:** Use keys starting with `sk_live_`
- **Testing:** Use keys starting with `sk_test_`
- Get your API key from your account manager at Musicmuni

---

## Permissions

### Android

1. **Add to `AndroidManifest.xml`:**

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

2. **Request runtime permission:**

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, can start recording
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
```

### iOS

1. **Add to `Info.plist`:**

```xml
<key>NSMicrophoneUsageDescription</key>
<string>This app needs microphone access for audio recording.</string>
```

2. **Request permission before recording:**

```swift
import AVFoundation

// Request permission
AVAudioSession.sharedInstance().requestRecordPermission { granted in
    if granted {
        // Permission granted, can start recording
    } else {
        // Permission denied
    }
}
```

---

## First Recording

Record audio with automatic encoding to M4A.

### Android (Kotlin + Compose)

```kotlin
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.musicmuni.sonix.SonixRecorder

@Composable
fun RecordingScreen(context: Context) {
    var recorder by remember { mutableStateOf<SonixRecorder?>(null) }

    // Observe recording state
    val isRecording by recorder?.isRecording?.collectAsState() ?: remember { mutableStateOf(false) }
    val duration by recorder?.duration?.collectAsState() ?: remember { mutableStateOf(0L) }
    val level by recorder?.level?.collectAsState() ?: remember { mutableStateOf(0f) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose { recorder?.release() }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Duration: ${formatDuration(duration)}")

        // Audio level indicator
        LinearProgressIndicator(
            progress = { level.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val outputPath = "${context.filesDir}/recording.m4a"
                    val newRecorder = SonixRecorder.create(
                        outputPath = outputPath,
                        format = "m4a",
                        quality = "voice"
                    )
                    recorder = newRecorder
                    newRecorder.start()
                },
                enabled = !isRecording
            ) {
                Text("Record")
            }

            Button(
                onClick = { recorder?.stop() },
                enabled = isRecording
            ) {
                Text("Stop")
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}
```

### iOS (Swift + SwiftUI)

```swift
import SwiftUI
import sonix
import AVFoundation

struct RecordingView: View {
    @State private var recorder: SonixRecorder?
    @State private var isRecording = false
    @State private var durationMs: Int64 = 0
    @State private var audioLevel: Float = 0

    @State private var recordingTask: Task<Void, Never>?
    @State private var durationTask: Task<Void, Never>?
    @State private var levelTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 16) {
            Text("Duration: \(formatDuration(durationMs))")

            // Audio level indicator
            ProgressView(value: Double(audioLevel), total: 1.0)

            HStack(spacing: 16) {
                Button("Record") {
                    startRecording()
                }
                .disabled(isRecording)

                Button("Stop") {
                    stopRecording()
                }
                .disabled(!isRecording)
            }
        }
        .padding()
        .onDisappear {
            recordingTask?.cancel()
            durationTask?.cancel()
            levelTask?.cancel()
            recorder?.release()
        }
    }

    private func startRecording() {
        Task {
            // Request permission
            let granted = await withCheckedContinuation { continuation in
                AVAudioSession.sharedInstance().requestRecordPermission { granted in
                    continuation.resume(returning: granted)
                }
            }
            guard granted else { return }

            // Create output path
            let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let outputPath = documentsPath.appendingPathComponent("recording.m4a").path

            // Create recorder
            let newRecorder = SonixRecorder.create(
                outputPath: outputPath,
                format: "m4a",
                quality: "voice"
            )

            await MainActor.run {
                recorder = newRecorder
            }

            // Start recording
            newRecorder.start()

            await MainActor.run {
                isRecording = true

                // Observe state
                recordingTask = newRecorder.observeIsRecording { self.isRecording = $0 }
                durationTask = newRecorder.observeDuration { self.durationMs = $0 }
                levelTask = newRecorder.observeLevel { self.audioLevel = $0 }
            }
        }
    }

    private func stopRecording() {
        recorder?.stop()
        isRecording = false
        audioLevel = 0
    }

    private func formatDuration(_ ms: Int64) -> String {
        let seconds = (ms / 1000) % 60
        let minutes = (ms / 1000) / 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}
```

---

## First Playback

Play audio files with volume and pitch control.

### Android (Kotlin + Compose)

```kotlin
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.musicmuni.sonix.SonixPlayer
import kotlinx.coroutines.launch

@Composable
fun PlaybackScreen(context: Context) {
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<SonixPlayer?>(null) }

    // Observe playback state
    val isPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentTime by player?.currentTime?.collectAsState() ?: remember { mutableStateOf(0L) }
    val duration = player?.duration ?: 0L

    // Load player
    LaunchedEffect(Unit) {
        // Create player (loads file automatically)
        player = SonixPlayer.create("/path/to/audio.m4a")
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Time display
        Text("${formatTime(currentTime)} / ${formatTime(duration)}")

        // Seek slider
        Slider(
            value = if (duration > 0) currentTime.toFloat() / duration else 0f,
            onValueChange = { player?.seek((it * duration).toLong()) },
            enabled = player != null
        )

        // Playback controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { player?.play() },
                enabled = player != null && !isPlaying
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
                onClick = { player?.stop() },
                enabled = player != null
            ) {
                Text("Stop")
            }
        }

        // Volume control
        var volume by remember { mutableFloatStateOf(1f) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume:")
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    player?.let { p -> p.volume = it }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Pitch control
        var pitch by remember { mutableFloatStateOf(0f) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pitch:")
            Slider(
                value = (pitch + 12f) / 24f,
                onValueChange = { pitch = (it * 24f) - 12f },
                onValueChangeFinished = { player?.let { p -> p.pitch = pitch } },
                modifier = Modifier.weight(1f)
            )
            Text("${pitch.toInt()} st")
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}
```

### iOS (Swift + SwiftUI)

```swift
import SwiftUI
import sonix

struct PlaybackView: View {
    @State private var player: SonixPlayer?
    @State private var isPlaying = false
    @State private var currentTimeMs: Int64 = 0
    @State private var durationMs: Int64 = 0
    @State private var volume: Float = 1.0
    @State private var pitch: Float = 0

    @State private var playingTask: Task<Void, Never>?
    @State private var timeTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 16) {
            // Time display
            Text("\(formatTime(currentTimeMs)) / \(formatTime(durationMs))")

            // Seek slider
            Slider(value: Binding(
                get: { durationMs > 0 ? Double(currentTimeMs) / Double(durationMs) : 0 },
                set: { newValue in
                    let seekPos = Int64(newValue * Double(durationMs))
                    player?.seek(positionMs: seekPos)
                }
            ))
            .disabled(player == nil)

            // Playback controls
            HStack(spacing: 16) {
                Button("Play") { player?.play() }
                    .disabled(player == nil || isPlaying)

                Button("Pause") { player?.pause() }
                    .disabled(!isPlaying)

                Button("Stop") {
                    player?.stop()
                    currentTimeMs = 0
                }
                .disabled(player == nil)
            }

            // Volume control
            HStack {
                Text("Volume:")
                Slider(value: Binding(
                    get: { Double(volume) },
                    set: { newValue in
                        volume = Float(newValue)
                        player?.volume = volume
                    }
                ))
                Text("\(Int(volume * 100))%")
            }

            // Pitch control
            HStack {
                Text("Pitch:")
                Slider(
                    value: Binding(
                        get: { Double(pitch + 12) / 24 },
                        set: { newValue in
                            pitch = Float(newValue * 24 - 12)
                        }
                    ),
                    onEditingChanged: { editing in
                        if !editing {
                            player?.pitch = pitch
                        }
                    }
                )
                Text("\(Int(pitch)) st")
            }
        }
        .padding()
        .task {
            await loadPlayer()
        }
        .onDisappear {
            playingTask?.cancel()
            timeTask?.cancel()
            player?.release()
        }
    }

    private func loadPlayer() async {
        do {
            // Create player (loads file automatically)
            let newPlayer = try await SonixPlayer.create(source: "/path/to/audio.m4a")

            await MainActor.run {
                player = newPlayer
                durationMs = newPlayer.duration

                // Observe state
                playingTask = newPlayer.observeIsPlaying { self.isPlaying = $0 }
                timeTask = newPlayer.observeCurrentTime { self.currentTimeMs = $0 }
            }
        } catch {
            print("Failed to load player: \(error)")
        }
    }

    private func formatTime(_ ms: Int64) -> String {
        let seconds = (ms / 1000) % 60
        let minutes = (ms / 1000) / 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
```

---

## Next Steps

Now that you've recorded and played audio, explore more features:

### Learn More

- **[User Guide](./User-Guide.md)** - Complete guide to all Sonix features
  - Multi-track mixing
  - Metronome
  - MIDI synthesis
  - Real-time audio processing

- **[API Reference](./API-Reference.md)** - Complete API documentation
  - All classes and methods
  - Parameters and return types
  - Platform-specific notes

- **[Advanced Topics](./Advanced-Topics.md)** - Deep dive into Sonix internals
  - Threading and coroutines
  - Performance optimization
  - Zero-allocation audio processing
  - Platform differences

- **[Migration Guide](./Migration-from-Legacy.md)** - Upgrading from audioiolib
  - Side-by-side comparisons
  - Breaking changes
  - API mapping

### Sample Apps

Check out the complete sample apps:
- **Android:** `samples/android/` - Jetpack Compose UI with all features
- **iOS:** `samples/ios/` - SwiftUI with all features

### Common Patterns

**Lifecycle Management (Compose):**
```kotlin
DisposableEffect(Unit) {
    onDispose {
        recorder?.release()
        player?.release()
    }
}
```

**Lifecycle Management (SwiftUI):**
```swift
.onDisappear {
    observerTasks.forEach { $0.cancel() }
    recorder?.release()
    player?.release()
}
```

**Error Handling:**
```kotlin
recorder.error.collect { error ->
    error?.let { showToast(it.message) }
}
```

**Loading Assets:**
```kotlin
// Android - Copy from assets
fun copyAssetToFile(context: Context, assetName: String): File {
    val file = File(context.filesDir, assetName)
    context.assets.open(assetName).use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}
```

```swift
// iOS - Copy from bundle
func copyAssetToFile(name: String, ext: String) -> String? {
    guard let assetPath = Bundle.main.path(forResource: name, ofType: ext) else {
        return nil
    }
    let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    let destPath = documentsPath.appendingPathComponent("\(name).\(ext)").path

    if !FileManager.default.fileExists(atPath: destPath) {
        try? FileManager.default.copyItem(atPath: assetPath, toPath: destPath)
    }

    return destPath
}
```

### Get Help

- **Issues:** Report bugs at [github.com/anthropics/sonix/issues](https://github.com/anthropics/sonix/issues)
- **API Documentation:** See [API-Reference.md](./API-Reference.md)
- **Examples:** See `samples/` directory

---

## Troubleshooting

### "Sonix not initialized" Error

**Solution:** Call `Sonix.initialize()` before using any Sonix features.

```kotlin
Sonix.initialize("sk_live_...", context)
```

### Permission Denied on Android

**Solution:** Request `RECORD_AUDIO` permission at runtime.

```kotlin
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
}
```

### Permission Denied on iOS

**Solution:** Add `NSMicrophoneUsageDescription` to `Info.plist` and request permission.

```swift
AVAudioSession.sharedInstance().requestRecordPermission { granted in
    // Handle result
}
```

### File Not Found

**Solution:** Verify file path is absolute and file exists.

```kotlin
val file = File(path)
if (!file.exists()) {
    println("File not found: $path")
}
```

### iOS Sample Rate Mismatch

**Solution:** Use `actualSampleRate` instead of requested rate.

```kotlin
// iOS hardware may return different rate
val actualRate = recorder.actualSampleRate
println("Requested: 16000, Actual: $actualRate")

// Use actual rate for encoding
SonixEncoder.encode(
    pcmData = data,
    sampleRate = actualRate,  // Use this!
    channels = 1,
    outputPath = path
)
```

### Playback No Sound

**Checklist:**
1. Verify file exists and is valid
2. Check volume is not 0
3. Verify audio session is active (iOS)
4. Check device volume is not muted
5. Enable logging: `Sonix.initializeLogging()`

---

## What's Next?

You're ready to build with Sonix! Explore:

- **Multi-track mixing** for karaoke or backing tracks
- **Real-time pitch detection** with Calibra integration
- **MIDI synthesis** for music education apps
- **Metronome** for practice applications

See the [User Guide](./User-Guide.md) for comprehensive examples.
