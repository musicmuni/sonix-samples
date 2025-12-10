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
7. **File Parsing** - Parse lesson files (.pitchPP, .notes, .trans) with data summaries

## Android Sample

### Building

```bash
cd samples/android

# Build debug APK
./gradlew :app:assembleDebug

# The APK will be at app/build/outputs/apk/debug/app-debug.apk
```

### Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 2.2.20
- Sonix AAR library (copy to `app/libs/`)

### Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── kotlin/.../sample/
│   │   │   ├── MainActivity.kt          # Main entry point
│   │   │   ├── RecordingSection.kt      # Recording with M4A/MP3, level metering
│   │   │   ├── PlaybackSection.kt       # Playback with pitch/volume/looping
│   │   │   ├── MultiTrackSection.kt     # Multi-track synchronized playback
│   │   │   ├── MidiSection.kt           # MIDI synthesis with FluidSynth
│   │   │   ├── MetronomeSection.kt      # Configurable metronome
│   │   │   ├── DecodingSection.kt       # Audio file decoding
│   │   │   └── ParserSection.kt         # Lesson file parsing
│   │   ├── assets/                      # Audio and data files
│   │   └── res/                         # Android resources
│   └── libs/
│       └── sonix.aar                    # Sonix library
├── build.gradle.kts
└── settings.gradle.kts
```

### Dependencies

The sample app requires the following dependencies (already configured):

```kotlin
dependencies {
    implementation(files("libs/sonix.aar"))

    // Required by Sonix
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.github.aakira:napier:2.7.1")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

## iOS Sample

### Building

1. Build the Sonix XCFramework first:

   ```bash
   cd library
   ./gradlew assembleXCFramework
   ```

2. Open the Xcode project:

   ```bash
   open samples/ios/SonixSample/SonixSample.xcodeproj
   ```

3. Link the XCFramework:
   - In Xcode, go to Project Settings > General > Frameworks
   - Add `library/build/XCFrameworks/release/sonix.xcframework`

4. Build and run on simulator or device

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
│   ├── SonixSampleApp.swift             # App entry point
│   ├── ContentView.swift                # Main view
│   ├── Sections/
│   │   ├── RecordingSection.swift       # Recording UI
│   │   ├── PlaybackSection.swift        # Playback UI
│   │   ├── MultiTrackSection.swift      # Multi-track UI
│   │   ├── MidiSection.swift            # MIDI synthesis UI
│   │   ├── MetronomeSection.swift       # Metronome UI
│   │   ├── DecodingSection.swift        # Audio decoding UI
│   │   └── ParserSection.swift          # File parsing UI
│   ├── Resources/                       # Audio assets
│   ├── Assets.xcassets/
│   └── Info.plist
```

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

### File Parsing

- **Parse .pitchPP**: Pitch contour with time/frequency arrays
- **Parse .notes**: Note annotations with labels and timing
- **Parse .trans**: JSON transcription with segments and lyrics

## Key API Patterns

### Lifecycle Management (Compose)

```kotlin
@Composable
fun Feature() {
    var player by remember { mutableStateOf<AudioPlayer?>(null) }

    // Initialize
    LaunchedEffect(Unit) {
        player = createAudioPlayer()
        // ...
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }
}
```

### Reactive State

```kotlin
// Observe with Flow collection
launch {
    player.isPlaying.collect { playing -> /* update UI */ }
}
launch {
    player.currentTime.collect { timeMs -> /* update UI */ }
}
```

### Zero-Allocation Audio Processing

```kotlin
val bufferPool = AudioBufferPool(poolSize = 4, bufferSize = 1024)

session.audioFlow.collect { buffer ->
    val floatBuffer = bufferPool.acquire()
    val sampleCount = buffer.fillFloatSamples(floatBuffer)

    // Process audio (e.g., RMS, pitch detection)

    bufferPool.release(floatBuffer)
}
```

### File Operations on IO Dispatcher

```kotlin
withContext(Dispatchers.IO) {
    player.load(filePath)
    // or
    AudioDecoder.decode(filePath)
}
```

## License

These samples are provided under the same license as the Sonix library.
