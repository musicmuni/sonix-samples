# Sonix Sample Apps

Sample applications demonstrating the Sonix KMP audio library for Android and iOS.

## Features Demonstrated

Both sample apps showcase all Sonix library capabilities:

1. **Recording** - Audio recording with real-time level metering, M4A and MP3 encoding, zero-allocation buffer processing
2. **Playback** - Audio playback with pitch shifting, volume control, looping, seeking, and fade effects
3. **Multi-Track** - Synchronized playback of multiple audio tracks with individual volume control and fading
4. **MIDI Synthesis** - Generate audio from MIDI notes using SoundFont files
5. **Metronome** - Configurable click track with visual beat indicator
6. **Audio Decoding** - Decode audio files to raw PCM data with metadata display

## Android Sample

### Setup Instructions

#### 1. Get the Sonix AAR

Contact your account manager at Musicmuni to obtain the Sonix AAR library file, then place it at:
```
android/app/libs/sonix.aar
```

#### 2. Configure API Key

**IMPORTANT:** You need a valid Sonix API key to use the library.

1. Navigate to `android/`
2. Copy the template file:
   ```bash
   cp local.properties.template local.properties
   ```
3. Open `local.properties` and replace `YOUR_API_KEY_HERE` with your actual API key

**Note:** `local.properties` is gitignored to prevent accidentally committing your API key.

#### 3. Build and Run

Open the project in Android Studio and run the app, or use the command line:

```bash
cd android
./gradlew :app:assembleDebug
```

### Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 2.2.20
- Jetpack Compose
- Sonix AAR library

### Project Structure

```
android/
├── app/
│   ├── src/main/kotlin/.../sample/
│   │   ├── MainActivity.kt          # Main entry point with mode switcher
│   │   ├── RecordingSection.kt      # Recording with M4A/MP3, level metering
│   │   ├── PlaybackSection.kt       # Playback with pitch/volume/looping
│   │   ├── MultiTrackSection.kt     # Multi-track synchronized playback
│   │   ├── MidiSection.kt           # MIDI synthesis with FluidSynth
│   │   ├── MetronomeSection.kt      # Configurable metronome
│   │   └── DecodingSection.kt       # Audio file decoding
│   ├── assets/                      # Audio and data files
│   └── libs/
│       └── sonix.aar                # Place Sonix library here
└── local.properties                 # API key configuration (gitignored)
```

### Troubleshooting

#### Build fails with "Sonix API key not found"

Make sure you've:
1. Created `local.properties` from the template
2. Added your API key: `sonix.apiKey=YOUR_KEY`

#### AAR not found

Make sure `sonix.aar` is placed in `android/app/libs/` directory.

## iOS Sample

### Setup Instructions

#### 1. Get the Sonix XCFramework

Contact your account manager at Musicmuni to obtain the Sonix XCFramework, then place it at:
```
ios/SonixSample/Frameworks/sonix.xcframework
```

#### 2. Configure API Key

**IMPORTANT:** You need a valid Sonix API key to use the library.

1. Navigate to `ios/SonixSample/SonixSample/`
2. Copy the template file:
   ```bash
   cp Config.swift.template Config.swift
   ```
3. Open `Config.swift` and replace `YOUR_API_KEY_HERE` with your actual API key

**Note:** `Config.swift` is gitignored to prevent accidentally committing your API key.

#### 3. Link the Framework in Xcode

1. Open `ios/SonixSample/SonixSample.xcodeproj` in Xcode
2. Select the project in the navigator
3. Go to the **SonixSample** target → **General** tab
4. Under **Frameworks, Libraries, and Embedded Content**, add `sonix.xcframework`
5. Set it to **Embed & Sign**

#### 4. Build and Run

Select a simulator or device and build the project in Xcode.

### Requirements

- iOS 16.0+
- Xcode 15.0+
- Swift 5.0
- Sonix XCFramework

### Project Structure

```
ios/SonixSample/
├── SonixSample.xcodeproj/
├── SonixSample/
│   ├── SonixSampleApp.swift         # App entry point
│   ├── ContentView.swift            # Main scrollable view
│   ├── Config.swift                 # API key configuration (gitignored)
│   ├── Sections/
│   │   ├── RecordingSection.swift   # Recording UI
│   │   ├── PlaybackSection.swift    # Playback UI
│   │   ├── MultiTrackSection.swift  # Multi-track UI
│   │   ├── MidiSection.swift        # MIDI synthesis UI
│   │   ├── MetronomeSection.swift   # Metronome UI
│   │   └── DecodingSection.swift    # Audio decoding UI
│   └── Resources/                   # Audio assets
│       ├── sample.m4a
│       ├── vocal.m4a
│       ├── sama_click.wav
│       ├── beat_click.wav
│       └── harmonium.sf2
└── Frameworks/
    └── sonix.xcframework            # Place Sonix framework here
```

### Troubleshooting

#### Build fails with "Cannot find 'Config' in scope"

Make sure you've:
1. Created `Config.swift` from the template
2. Added your API key to the file
3. Added `Config.swift` to the Xcode project (right-click → Add Files)

#### Framework not found

Make sure `sonix.xcframework` is:
1. Placed in `ios/SonixSample/Frameworks/`
2. Added to the Xcode project under **Frameworks, Libraries, and Embedded Content**
3. Set to **Embed & Sign**

## Getting a Sonix License

To use the Sonix library in your own projects, you need:

1. **API Key** - Required for SDK initialization and usage tracking
2. **Library Files** - Platform-specific distribution files:
   - Android: `sonix-YYYY.MMDD.HHMM.aar`
   - iOS: `sonix-YYYY.MMDD.HHMM.xcframework`

Contact your account manager at Musicmuni for licensing information.

## Audio Assets

Both sample apps include the following audio and data files:

| File | Purpose |
|------|---------|
| `sample.m4a` | Playback and decoding demo |
| `vocal.m4a` | Multi-track vocal layer |
| `sama_click.wav` | Metronome downbeat sound |
| `beat_click.wav` | Metronome beat sound |
| `harmonium.sf2` | SoundFont for MIDI synthesis |
| `Alankaar 01.pitchPP` | Pitch contour data for parsing demo |
| `Alankaar 01.notes` | Note annotations for parsing demo |
| `Alankaar 01.trans` | Transcription JSON for parsing demo |

## API Modes (Android)

The Android sample app includes a mode switcher to demonstrate two API approaches:

### Simple API Mode

- **Best for**: Quick prototyping and simple use cases
- **Characteristics**: Simplified lifecycle management, automatic resource cleanup
- **Use when**: You want minimal code and don't need fine-grained control

Example features shown:

- Simple recording with automatic encoder setup
- Basic playback with minimal configuration
- Streamlined multi-track playback

### Advanced API Mode

- **Best for**: Production apps requiring full control
- **Characteristics**: Explicit resource management, detailed configuration options
- **Use when**: You need fine-grained control over audio processing, custom encoders, or advanced features

Example features shown:

- Manual lifecycle management with proper cleanup
- Detailed configuration of recording sessions
- Custom buffer processing with zero-allocation patterns
- Advanced playback controls with fade effects

**Toggle between modes** using the segmented control at the top of the app to compare the two approaches.

## Usage Notes

### Recording

- Grant microphone permission when prompted
- Select format: M4A (AAC) or MP3 (LAME)
- Press Record to start, Stop & Save to encode and save
- Real-time audio level meter shows RMS amplitude
- Buffer pool status shows zero-allocation processing health
- After saving, use Play/Pause/Stop to preview the recording

### Playback

- Audio loads automatically from assets
- Seek by dragging the slider
- Adjust volume: 0-100%
- Adjust pitch: -12 to +12 semitones
- Loop count: 1 = play once, 2 = repeat once, 3 = repeat twice, ∞ = infinite
- Fade In/Out buttons demonstrate smooth volume transitions

### Multi-Track

- Two tracks loaded: backing and vocal
- Individual volume sliders for each track
- Tracks remain synchronized during seek
- Fade Vocal buttons demonstrate per-track fading

### MIDI Synthesis

- Generates a C major scale (C4-C5) using the harmonium SoundFont
- Output is saved as a WAV file in the cache directory
- Play button plays the generated audio

### Metronome

- BPM range: 60-200 (adjustable with slider)
- Beats per cycle: 3, 4, 6, or 8 (select to reinitialize)
- Volume: 0-100%
- Sama (downbeat, beat 0) is highlighted in red
- Visual beat indicators show current position

### Audio Decoding

- Press "Decode sample.m4a" to decode the asset file
- Displays: sample rate, channels, duration, PCM data size

## Key API Patterns

### Android (Compose) - Initialization

```kotlin
import com.musicmuni.sonix.Sonix

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Sonix with your API key
        Sonix.initialize(BuildConfig.SONIX_API_KEY, this)

        setContent {
            // Your app content
        }
    }
}
```

### Android - Lifecycle Management

```kotlin
@Composable
fun AudioFeature() {
    var player by remember { mutableStateOf<AudioPlayer?>(null) }

    // Initialize
    LaunchedEffect(Unit) {
        player = createAudioPlayer()
        player?.load(filePath)
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }
}
```

### Android - Reactive State

```kotlin
// Observe player state with Flow
val isPlaying by player.isPlaying.collectAsState(initial = false)
val currentTime by player.currentTime.collectAsState(initial = 0L)

// Use in Compose UI
Button(onClick = { player.play() }) {
    Text(if (isPlaying) "Pause" else "Play")
}
```

### iOS (SwiftUI) - Initialization

```swift
import sonix

@main
struct MyApp: App {
    init() {
        // Initialize Sonix with your API key
        Sonix.initialize(apiKey: Config.sonixAPIKey)

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

### iOS - Observing State

Kotlin StateFlow is exposed as an async sequence in Swift:

```swift
Task {
    for await playing in player.isPlaying {
        await MainActor.run {
            self.isPlaying = playing as! Bool
        }
    }
}
```

### iOS - Async Operations

Kotlin suspend functions become async in Swift:

```swift
let loaded = try await player.load(path: audioPath)
try await session.stopAndSave(outputPath: outputPath)
```

### Zero-Allocation Audio Processing

```kotlin
val bufferPool = AudioBufferPool(poolSize = 4, bufferSize = 1024)

session.audioFlow.collect { buffer ->
    val floatBuffer = bufferPool.acquire()
    val sampleCount = buffer.fillFloatSamples(floatBuffer)

    // Process audio (e.g., RMS calculation, pitch detection)
    val rms = calculateRMS(floatBuffer, sampleCount)

    bufferPool.release(floatBuffer)
}
```

### File Operations on IO Dispatcher

```kotlin
// Always perform file I/O on the IO dispatcher
withContext(Dispatchers.IO) {
    player.load(filePath)
    // or
    val decodedData = AudioDecoder.decode(filePath)
}
```

## Additional Resources

For full API documentation and integration guides, contact your Musicmuni account manager.

## License

These samples demonstrate usage of the Sonix KMP audio library. The Sonix library is proprietary software licensed by Musicmuni.
