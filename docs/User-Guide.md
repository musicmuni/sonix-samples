# Sonix User Guide

Comprehensive guide to all Sonix features with working code examples.

---

## Table of Contents

1. [Recording](#recording)
2. [Playback](#playback)
3. [Multi-Track Mixing](#multi-track-mixing)
4. [Metronome](#metronome)
5. [MIDI Synthesis](#midi-synthesis)
6. [Lesson Synthesis](#lesson-synthesis)
7. [Encoding & Decoding](#encoding--decoding)
8. [Error Handling](#error-handling)

---

## Recording

Record audio with automatic encoding to M4A or MP3.

### Basic Recording

**Kotlin (Android):**
```kotlin
import com.musicmuni.sonix.SonixRecorder

// Create recorder with simple API
val recorder = SonixRecorder.create(
    outputPath = "${context.filesDir}/recording.m4a",
    format = "m4a",
    quality = "voice"  // 16kHz mono
)

// Start recording
recorder.start()

// Observe duration
recorder.duration.collect { ms ->
    updateDuration(ms)
}

// Observe audio level
recorder.level.collect { level ->
    updateLevelMeter(level)  // 0.0 to 1.0
}

// Stop and save
recorder.stop()

// Cleanup
recorder.release()
```

**Swift (iOS):**
```swift
import sonix

// Create recorder
let outputPath = "\(documentsPath)/recording.m4a"
let recorder = SonixRecorder.create(
    outputPath: outputPath,
    format: "m4a",
    quality: "voice"
)

// Start recording
recorder.start()

// Observe state with type-safe observers
let durationTask = recorder.observeDuration { ms in
    self.updateDuration(ms)
}

let levelTask = recorder.observeLevel { level in
    self.updateLevelMeter(level)
}

// Stop and save
recorder.stop()

// Cleanup
durationTask.cancel()
levelTask.cancel()
recorder.release()
```

### Advanced Recording with Builder

Use Builder for custom sample rates, bitrates, and callbacks.

**Kotlin:**
```kotlin
val recorder = SonixRecorder.Builder()
    .outputPath("${context.filesDir}/recording.mp3")
    .format("mp3")
    .sampleRate(16000)
    .channels(1)
    .bitrate(128000)
    .onRecordingStarted {
        println("Recording started!")
    }
    .onRecordingStopped { path ->
        println("Saved to: $path")
    }
    .onError { error ->
        showToast("Error: $error")
    }
    .build()

recorder.start()
```

**Swift:**
```swift
// Note: Swift Builder not yet implemented, use create() for now
// For custom sample rates, use create() and configure after
```

### Quality Presets

Three predefined quality levels:

| Quality | Sample Rate | Channels | Use Case |
|---------|-------------|----------|----------|
| `"voice"` | 16kHz | Mono | Voice recording, smallest files |
| `"standard"` | 44.1kHz | Mono | Balanced quality/size |
| `"high"` | 44.1kHz | Stereo | Music, highest quality |

**Example:**
```kotlin
val highQuality = SonixRecorder.create(
    outputPath = "/path/to/recording.m4a",
    format = "m4a",
    quality = "high"
)
```

### Format Selection

Sonix supports two formats:

**M4A (AAC)** - Recommended
- Better quality at same bitrate
- Native encoding on Android (MediaCodec) and iOS (AVFoundation)
- Smaller files

**MP3 (LAME)**
- Wider compatibility
- Encoded via LAME library
- Slightly larger files

**Example:**
```kotlin
// M4A (recommended)
val m4aRecorder = SonixRecorder.create(outputPath, "m4a", "voice")

// MP3 (for compatibility)
val mp3Recorder = SonixRecorder.create(outputPath, "mp3", "voice")
```

### Audio Level Monitoring

Monitor real-time audio levels for visual feedback.

**Kotlin:**
```kotlin
recorder.level.collect { level ->
    // level is RMS from 0.0 (silence) to 1.0 (loud)
    levelMeterView.setLevel(level)
}
```

**Swift:**
```swift
let levelTask = recorder.observeLevel { level in
    self.levelMeter.value = Double(level)
}
```

**Visual Example (Compose):**
```kotlin
val level by recorder.level.collectAsState()

LinearProgressIndicator(
    progress = { level.coerceIn(0f, 1f) },
    modifier = Modifier.fillMaxWidth().height(8.dp)
)
```

### Real-Time Audio Buffer Access

Access raw PCM buffers for pitch detection, visualization, or custom DSP.

**Kotlin:**
```kotlin
recorder.audioBuffers.collect { buffer ->
    // Convert to float samples
    val floats = FloatArray(buffer.sampleCount)
    buffer.fillFloatSamples(floats)

    // Pass to Calibra for pitch detection
    calibra.detectPitch(floats)

    // Or process manually
    processAudio(floats)
}
```

**Zero-Allocation Pattern (Performance-Critical):**
```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

recorder.audioBuffers.collect { buffer ->
    val floatBuffer = pool.acquire()  // Reuse pre-allocated buffer
    val sampleCount = buffer.fillFloatSamples(floatBuffer)

    // Process without allocations
    calibra.detectPitch(floatBuffer, sampleCount)

    pool.release(floatBuffer)  // Return to pool
}

// Check if pool was exhausted
if (recorder.bufferPoolWasExhausted) {
    println("Warning: Buffer pool too small, allocations occurred")
}
```

### Recording with Playback Sync

Record vocals while playing backing track (karaoke scenario).

**Kotlin:**
```kotlin
// Create backing track player
val backing = SonixPlayer.create("/path/to/backing.mp3")

// Create recorder and sync with backing
val recorder = SonixRecorder.create(outputPath, "m4a", "voice")
recorder.setPlaybackSyncProvider(backing)

// Start both
backing.play()
recorder.start()

// Recording timestamps now match backing track time
val syncedTime = recorder.synchronizedTimeMs
```

**Use Case:** Ensures recorded audio aligns with backing track when mixed later.

---

## Playback

Play audio files with volume control, pitch shifting, looping, and fade effects.

### Basic Playback

**Kotlin (Android):**
```kotlin
import com.musicmuni.sonix.SonixPlayer

// Create player (loads file automatically)
val player = SonixPlayer.create("/path/to/audio.m4a")

// Control playback
player.play()
player.pause()
player.stop()
player.seek(5000)  // Seek to 5 seconds

// Observe state
player.isPlaying.collect { playing ->
    updatePlayButton(playing)
}

player.currentTime.collect { timeMs ->
    updateProgressBar(timeMs)
}

// Get duration
val durationMs = player.duration

// Cleanup
player.release()
```

**Swift (iOS):**
```swift
import sonix

// Create player (async loading)
let player = try await SonixPlayer.create(source: "/path/to/audio.m4a")

// Control playback
player.play()
player.pause()
player.stop()
player.seek(positionMs: 5000)

// Observe state with type-safe observers
let playingTask = player.observeIsPlaying { isPlaying in
    self.updatePlayButton(isPlaying)
}

let timeTask = player.observeCurrentTime { timeMs in
    self.updateProgressBar(timeMs)
}

// Get duration
let durationMs = player.duration

// Cleanup
playingTask.cancel()
timeTask.cancel()
player.release()
```

### Volume Control

Set volume from 0.0 (mute) to 1.0 (full).

**Property-Style (Immediate):**
```kotlin
player.volume = 0.8f  // 80% volume
```

**Fade In:**
```kotlin
// Kotlin
lifecycleScope.launch {
    player.fadeIn(targetVolume = 1.0f, durationMs = 1000)
}
```

```swift
// Swift
Task {
    try await player.fadeIn(targetVolume: 1.0, durationMs: 1000)
}
```

**Fade Out:**
```kotlin
lifecycleScope.launch {
    player.fadeOut(durationMs = 1000)
}
```

### Pitch Shifting

Shift pitch from -12 to +12 semitones (1 octave down/up).

**Property-Style:**
```kotlin
player.pitch = -2f  // Down 2 semitones (1 whole step)
player.pitch = 3f   // Up 3 semitones (minor third)
player.pitch = 0f   // Original pitch
```

**Example (Transpose to Different Key):**
```kotlin
// Original song in C, transpose to Bb (down 2 semitones)
player.pitch = -2f
player.play()
```

**Platform Notes:**
- Android: Uses SoundTouch library (high quality)
- iOS: Uses AVAudioUnitTimePitch (native Apple)
- Real-time adjustment (no re-encoding needed)

### Looping

Set how many times to play the audio.

**Property-Style:**
```kotlin
player.loopCount = 1    // Play once (default)
player.loopCount = 3    // Play three times total
player.loopCount = -1   // Infinite loop
```

**With Callback (Builder):**
```kotlin
val player = SonixPlayer.Builder()
    .source("/path/to/audio.m4a")
    .loopCount(3)
    .onLoopComplete { currentLoop, totalLoops ->
        println("Loop $currentLoop of $totalLoops complete")
    }
    .onPlaybackComplete {
        println("All loops finished!")
    }
    .build()
```

**Important:** `loopCount = 1` means "play once", not "play then loop once". Set to 2 for "play twice total".

### Seek and Progress

**Seek to Position:**
```kotlin
player.seek(30000)  // Jump to 30 seconds
```

**Progress Bar (Compose):**
```kotlin
val currentTime by player.currentTime.collectAsState()
val duration = player.duration

Slider(
    value = if (duration > 0) currentTime.toFloat() / duration else 0f,
    onValueChange = { progress ->
        player.seek((progress * duration).toLong())
    }
)
```

**Progress Bar (SwiftUI):**
```swift
@State private var currentTime: Int64 = 0
let duration = player.duration

Slider(
    value: Binding(
        get: { duration > 0 ? Double(currentTime) / Double(duration) : 0 },
        set: { progress in
            player.seek(positionMs: Int64(progress * Double(duration)))
        }
    )
)
.task {
    timeTask = player.observeCurrentTime { self.currentTime = $0 }
}
```

### Loading from Raw PCM

Load audio from raw PCM data instead of file.

**Kotlin:**
```kotlin
val rawData = SonixDecoder.decode("/path/to/audio.mp3")
if (rawData != null) {
    val player = SonixPlayer.Builder()
        .source("")  // Not used for PCM
        .build()

    player.load(
        data = rawData.audioData,
        sampleRate = rawData.sampleRate,
        channels = rawData.numChannels
    )

    player.play()
}
```

---

## Multi-Track Mixing

Synchronized playback of multiple audio tracks with per-track volume control.

### Basic Multi-Track Playback

**Kotlin:**
```kotlin
import com.musicmuni.sonix.SonixMixer

// Create mixer
val mixer = SonixMixer.create()

// Add tracks from files (auto-decodes!)
mixer.addTrack("backing", "/path/to/backing.m4a")
mixer.addTrack("vocal", "/path/to/vocal.m4a")

// Set per-track volumes
mixer.setTrackVolume("backing", 0.8f)
mixer.setTrackVolume("vocal", 1.0f)

// Play all tracks synchronized
mixer.play()

// Observe state
mixer.isPlaying.collect { playing ->
    updatePlayButton(playing)
}

mixer.currentTime.collect { timeMs ->
    updateProgressBar(timeMs)
}

// Duration is the longest track
val duration = mixer.duration

// Cleanup
mixer.release()
```

**Swift:**
```swift
// Create mixer
let mixer = SonixMixer.create()

// Add tracks
mixer.addTrack(name: "backing", filePath: backingPath)
mixer.addTrack(name: "vocal", filePath: vocalPath)

// Set volumes
mixer.setTrackVolume(name: "backing", volume: 0.8)
mixer.setTrackVolume(name: "vocal", volume: 1.0)

// Play synchronized
mixer.play()

// Observe state
playingTask = mixer.observeIsPlaying { self.isPlaying = $0 }
timeTask = mixer.observeCurrentTime { self.currentTime = $0 }

// Cleanup
mixer.release()
```

### Per-Track Volume Control

**Immediate Volume Change:**
```kotlin
mixer.setTrackVolume("backing", 0.5f)
```

**Fade Track Volume:**
```kotlin
// Fade from current volume to 0.2 over 500ms
mixer.fadeTrackVolume("vocal", targetVolume = 0.2f, durationMs = 500)

// Fade from specific start to end volume
mixer.fadeTrackVolume(
    name = "vocal",
    startVolume = 1.0f,
    endVolume = 0.0f,
    durationMs = 1000
)
```

**Example (Ducking Backing Track):**
```kotlin
// Lower backing track when user is singing
recorder.start()
mixer.fadeTrackVolume("backing", 0.3f, 300)  // Duck to 30%

// Restore when done
recorder.stop()
mixer.fadeTrackVolume("backing", 0.8f, 300)  // Back to 80%
```

### Track Management

**Check if Track Exists:**
```kotlin
if (mixer.hasTrack("vocal")) {
    mixer.removeTrack("vocal")
}
```

**Get All Tracks:**
```kotlin
val trackNames = mixer.getTrackNames()
println("Loaded tracks: $trackNames")
```

**Remove Track:**
```kotlin
mixer.removeTrack("backing")
```

**Add Track from Raw PCM:**
```kotlin
val audioData = SonixDecoder.decode("/path/to/track.mp3")
if (audioData != null) {
    mixer.addTrack(
        name = "drums",
        data = audioData.audioData,
        sampleRate = audioData.sampleRate,
        channels = audioData.numChannels
    )
}
```

### Looping All Tracks

**Property-Style:**
```kotlin
mixer.loopCount = 3    // Loop all tracks 3 times
mixer.loopCount = -1   // Infinite loop
```

**Monitor Loop Progress:**
```kotlin
println("Completed loops: ${mixer.completedLoops}")
```

**With Callbacks (Builder):**
```kotlin
val mixer = SonixMixer.Builder()
    .loopCount(3)
    .onLoopComplete { loopIndex ->
        println("Loop $loopIndex complete")
    }
    .onPlaybackComplete {
        println("All loops finished!")
    }
    .build()
```

### Synchronized Seek

Seeking applies to all tracks simultaneously.

```kotlin
mixer.seek(10000)  // All tracks jump to 10 seconds
```

---

## Metronome

Metronome for practice with configurable BPM and beats per cycle.

### Basic Metronome

**Kotlin:**
```kotlin
import com.musicmuni.sonix.SonixMetronome

// Create metronome
val metronome = SonixMetronome.create(
    samaSamplePath = "/path/to/sama_click.wav",  // Downbeat sample
    beatSamplePath = "/path/to/beat_click.wav",  // Regular beat sample
    bpm = 120f,
    beatsPerCycle = 4  // 4/4 time
)

// Observe current beat (0-based, wraps at beatsPerCycle)
metronome.currentBeat.collect { beat ->
    updateBeatIndicator(beat)  // 0, 1, 2, 3, 0, 1, 2, 3...
}

// Start metronome
metronome.start()

// Change BPM while running
metronome.setBpm(140f)

// Adjust volume
metronome.volume = 0.5f

// Stop
metronome.stop()

// Cleanup
metronome.release()
```

**Swift:**
```swift
// Create metronome
let metronome = SonixMetronome.create(
    samaSamplePath: samaPath,
    beatSamplePath: beatPath,
    bpm: 120,
    beatsPerCycle: 4
)

// Observe current beat
beatTask = metronome.observeCurrentBeat { beat in
    self.updateBeatIndicator(beat)
}

// Start
metronome.start()

// Change BPM
metronome.setBpm(bpm: 140)

// Adjust volume
metronome.volume = 0.5

// Stop
metronome.stop()

// Cleanup
beatTask.cancel()
metronome.release()
```

### BPM Control

**Set BPM (30-300 range):**
```kotlin
metronome.setBpm(120f)  // Takes effect on next beat
```

**Property Access (Read-Only):**
```kotlin
val currentBpm by metronome.bpm.collectAsState()
```

### Beat Tracking

**Visual Beat Indicator (Compose):**
```kotlin
val currentBeat by metronome.currentBeat.collectAsState()
val isPlaying by metronome.isPlaying.collectAsState()

Row {
    (0 until 4).forEach { beat ->
        val isActive = beat == currentBeat && isPlaying
        val isSama = beat == 0

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        isActive && isSama -> Color.Red
                        isActive -> Color.Green
                        isSama -> Color.Red.copy(alpha = 0.3f)
                        else -> Color.Gray.copy(alpha = 0.3f)
                    }
                )
        )
    }
}
```

### Changing Beats Per Cycle

To change beats per cycle, you must create a new metronome instance.

```kotlin
// Stop and release old metronome
metronome.stop()
metronome.release()

// Create new metronome with different beat count
val newMetronome = SonixMetronome.create(
    samaSamplePath = samaPath,
    beatSamplePath = beatPath,
    bpm = 120f,
    beatsPerCycle = 6  // Now 6/8 time
)
```

### Beat Callback

Use Builder for beat callbacks.

```kotlin
val metronome = SonixMetronome.Builder()
    .samaSamplePath(samaPath)
    .beatSamplePath(beatPath)
    .bpm(120f)
    .beatsPerCycle(4)
    .onBeat { beatIndex ->
        if (beatIndex == 0) {
            println("Downbeat!")
        } else {
            println("Beat $beatIndex")
        }
    }
    .build()
```

---

## MIDI Synthesis

Generate audio from MIDI using SoundFont files.

### Basic MIDI Synthesis

**Kotlin:**
```kotlin
import com.musicmuni.sonix.SonixMidiSynthesizer

// Create synthesizer
val synth = SonixMidiSynthesizer.create("/path/to/soundfont.sf2")

// Synthesize MIDI file to WAV
val success = synth.synthesize(
    midiPath = "/path/to/input.mid",
    outputPath = "/path/to/output.wav"
)

if (success) {
    println("Synthesis complete!")
}
```

**Swift:**
```swift
let synth = SonixMidiSynthesizer.create(soundFontPath: soundFontPath)

let success = synth.synthesize(
    midiPath: midiPath,
    outputPath: outputPath
)
```

### Synthesize from Note List

Generate audio from programmatically created notes.

**Kotlin:**
```kotlin
import com.musicmuni.sonix.model.MidiNote

// Create C major scale
val notes = listOf(
    MidiNote(note = 60, startTime = 0f, endTime = 400f),     // C4
    MidiNote(note = 62, startTime = 500f, endTime = 900f),   // D4
    MidiNote(note = 64, startTime = 1000f, endTime = 1400f), // E4
    MidiNote(note = 65, startTime = 1500f, endTime = 1900f), // F4
    MidiNote(note = 67, startTime = 2000f, endTime = 2400f), // G4
    MidiNote(note = 69, startTime = 2500f, endTime = 2900f), // A4
    MidiNote(note = 71, startTime = 3000f, endTime = 3400f), // B4
    MidiNote(note = 72, startTime = 3500f, endTime = 4400f)  // C5
)

val success = synth.synthesizeFromNotes(
    notes = notes,
    outputPath = "/path/to/scale.wav"
)
```

**MIDI Note Numbers:**
- 60 = C4 (Middle C)
- 61 = C#4
- 62 = D4
- ...
- 72 = C5

**Timing:** `startTime` and `endTime` are absolute milliseconds from start.

### Synthesize from Pitch Contour

Generate reference audio from pitch contour file (lesson scenarios).

**Pitch File Format:**
```
startSec endSec freqHz
0.0 0.5 261.63
0.5 1.0 293.66
1.0 1.5 329.63
```

**Kotlin:**
```kotlin
val success = synth.synthesizeFromPitchFile(
    pitchPath = "/path/to/lesson_pitch.txt",
    outputPath = "/path/to/reference.wav",
    lessonTonicHz = 261.63f,  // C4
    parentTonicHz = 261.63f   // C4
)
```

**Use Case:** Generate reference audio for Indian classical music lessons.

### Advanced Configuration (Builder)

**Kotlin:**
```kotlin
val synth = SonixMidiSynthesizer.Builder()
    .soundFontPath("/path/to/soundfont.sf2")
    .sampleRate(48000)  // Higher quality
    .onError { error -> println("Synthesis error: $error") }
    .build()
```

### FluidSynth Version

Check FluidSynth library version.

```kotlin
println("FluidSynth version: ${synth.version}")
```

---

## Lesson Synthesis

Synthesize Indian classical music lessons from svara sequences.

### Basic Lesson Synthesis

**Kotlin:**
```kotlin
import com.musicmuni.sonix.SonixLessonSynthesizer
import com.musicmuni.sonix.model.LessonSvara

// Define svaras
val svaras = listOf(
    LessonSvara(
        svaraName = "Sa",
        svaraLabel = "S",
        svaraAudioFilePath = "/path/to/sa.wav",
        numBeats = 2,
        numSamplesConsonant = 100
    ),
    LessonSvara(
        svaraName = "Re",
        svaraLabel = "R",
        svaraAudioFilePath = "/path/to/re.wav",
        numBeats = 2,
        numSamplesConsonant = 100
    ),
    LessonSvara(
        svaraName = "Ga",
        svaraLabel = "G",
        svaraAudioFilePath = "/path/to/ga.wav",
        numBeats = 1,
        numSamplesConsonant = 100
    )
)

// Create synthesizer
val synth = SonixLessonSynthesizer.create(
    svaras = svaras,
    beatLengthMs = 500  // 500ms per beat
)

// Load audio files
lifecycleScope.launch {
    if (synth.loadAudio()) {
        // Synthesize lesson
        val audioData = synth.synthesize()

        if (audioData != null) {
            // Save to file
            SonixEncoder.encode(
                data = audioData,
                outputPath = "/path/to/lesson.m4a"
            )
        }
    }
}
```

### Advanced Configuration (Builder)

**Kotlin:**
```kotlin
val synth = SonixLessonSynthesizer.Builder()
    .svaras(svaraList)
    .beatLengthMs(500)
    .silenceBeats(start = 2, end = 2)  // Silence padding
    .sampleRate(44100)
    .onError { error -> println("Error: $error") }
    .build()
```

### Monitoring Load Progress

**Kotlin:**
```kotlin
val isLoading by synth.isLoading.collectAsState()
val isLoaded by synth.isLoaded.collectAsState()
val error by synth.error.collectAsState()

when {
    isLoading -> Text("Loading audio files...")
    error != null -> Text("Error: ${error?.message}")
    isLoaded -> Text("Ready to synthesize")
}
```

### Synchronous Loading

For non-coroutine contexts.

```kotlin
if (synth.loadAudioSync()) {
    val audioData = synth.synthesize()
    // Process audio...
}
```

---

## Encoding & Decoding

Convert between compressed audio formats and raw PCM.

### Decoding Audio Files

**Kotlin:**
```kotlin
import com.musicmuni.sonix.SonixDecoder

val audioData = SonixDecoder.decode("/path/to/audio.mp3")

if (audioData != null) {
    println("Sample rate: ${audioData.sampleRate}")
    println("Channels: ${audioData.numChannels}")
    println("Duration: ${audioData.durationMilliSecs}ms")

    // Access raw PCM bytes
    val pcmBytes = audioData.audioData

    // Use with player
    player.load(
        data = audioData.audioData,
        sampleRate = audioData.sampleRate,
        channels = audioData.numChannels
    )
}
```

**Supported Formats:**
- WAV, MP3, M4A, AAC
- Any format supported by platform (MediaCodec on Android, AVFoundation on iOS)

### Encoding to M4A/AAC

**From AudioRawData:**
```kotlin
import com.musicmuni.sonix.SonixEncoder

val rawData = SonixDecoder.decode("/path/to/input.wav")
if (rawData != null) {
    val success = SonixEncoder.encode(
        data = rawData,
        outputPath = "/path/to/output.m4a",
        format = "m4a",
        bitrateKbps = 128
    )
}
```

**From Float Samples:**
```kotlin
// Float samples in range [-1.0, 1.0]
val samples = FloatArray(sampleCount) { i ->
    sin(2 * PI * 440 * i / 44100).toFloat()  // 440Hz sine wave
}

val success = SonixEncoder.encode(
    samples = samples,
    sampleRate = 44100,
    channels = 1,
    outputPath = "/path/to/output.m4a",
    format = "m4a",
    bitrateKbps = 128
)
```

**From PCM Bytes:**
```kotlin
val success = SonixEncoder.encode(
    pcmData = pcmBytes,
    sampleRate = 16000,
    channels = 1,
    outputPath = "/path/to/output.m4a",
    format = "m4a",
    bitrateKbps = 128
)
```

### Encoding to MP3

**From AudioRawData:**
```kotlin
val rawData = SonixDecoder.decode("/path/to/input.wav")
if (rawData != null) {
    val success = SonixEncoder.encode(
        data = rawData,
        outputPath = "/path/to/output.mp3",
        format = "mp3",
        bitrateKbps = 128
    )
}
```

### Format Availability

Check if encoding format is supported.

```kotlin
if (SonixEncoder.isFormatAvailable("mp3")) {
    // MP3 encoding is available
}

if (SonixEncoder.isFormatAvailable("m4a")) {
    // M4A encoding is available (always true on Android/iOS)
}
```

### Transcode Audio

Convert between formats.

```kotlin
// MP3 → M4A
val rawData = SonixDecoder.decode("/path/to/input.mp3")
if (rawData != null) {
    SonixEncoder.encode(
        data = rawData,
        outputPath = "/path/to/output.m4a",
        format = "m4a"
    )
}

// M4A → MP3
val rawData2 = SonixDecoder.decode("/path/to/input.m4a")
if (rawData2 != null) {
    SonixEncoder.encode(
        data = rawData2,
        outputPath = "/path/to/output.mp3",
        format = "mp3"
    )
}
```

---

## Error Handling

Sonix uses nullable returns and StateFlows for error handling (no exceptions).

### Check Return Values

Most operations return `Boolean` or nullable results.

```kotlin
// Boolean return
val success = recorder.start()
if (!success) {
    showError("Failed to start recording")
}

// Nullable return
val audioData = SonixDecoder.decode(path)
if (audioData == null) {
    showError("Failed to decode audio")
}
```

### Observe Error StateFlow

All components expose an `error` StateFlow.

**Kotlin:**
```kotlin
recorder.error.collect { error ->
    error?.let { err ->
        showToast("Recording error: ${err.message}")
    }
}
```

**Swift:**
```swift
errorTask = recorder.observeError { error in
    if let err = error {
        self.showAlert("Error: \(err.message)")
    }
}
```

### Error Callbacks (Builder)

Set error callbacks when creating components.

```kotlin
val recorder = SonixRecorder.Builder()
    .outputPath(path)
    .onError { error ->
        showToast("Error: $error")
    }
    .build()
```

### Common Errors

**Initialization Required:**
```kotlin
// MUST call before using any Sonix features
Sonix.initialize("sk_live_...", context)
```

**Microphone Permission:**
```kotlin
// Android - check permission
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
}
```

```swift
// iOS - request permission
AVAudioSession.sharedInstance().requestRecordPermission { granted in
    if granted {
        // Start recording
    }
}
```

**File Not Found:**
```kotlin
val file = File(path)
if (!file.exists()) {
    showError("Audio file not found: $path")
    return
}

val player = SonixPlayer.create(path)
```

**Invalid Sample Rate (iOS):**
```kotlin
// iOS hardware may not support requested sample rate
println("Requested: 16000, Actual: ${recorder.actualSampleRate}")

// Use actualSampleRate for encoding
SonixEncoder.encode(
    pcmData = data,
    sampleRate = recorder.actualSampleRate,  // Use actual, not requested!
    channels = 1,
    outputPath = outputPath
)
```

---

## Next Steps

- **Getting Started**: See [Getting-Started.md](./Getting-Started.md) for installation and setup
- **API Reference**: See [API-Reference.md](./API-Reference.md) for complete API documentation
- **Advanced Topics**: See [Advanced-Topics.md](./Advanced-Topics.md) for threading, performance tuning, and platform-specific details
- **Migration**: See [Migration-from-Legacy.md](./Migration-from-Legacy.md) for upgrading from audioiolib
