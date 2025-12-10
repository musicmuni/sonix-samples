# Sonix API Reference

Complete API documentation for all public Sonix classes.

---

## Table of Contents

1. [Sonix (SDK Utilities)](#sonix-sdk-utilities)
2. [SonixRecorder](#sonixrecorder)
3. [SonixPlayer](#sonixplayer)
4. [SonixMixer](#sonixmixer)
5. [SonixMetronome](#sonixmetronome)
6. [SonixMidiSynthesizer](#sonixmidisynthesizer)
7. [SonixLessonSynthesizer](#sonixlessonsynthesizer)
8. [SonixEncoder](#sonixencoder)
9. [SonixDecoder](#sonixdecoder)
10. [Data Models](#data-models)

---

## Sonix (SDK Utilities)

SDK initialization, logging, buffer pools, and utility enums.

### Methods

#### `initialize(apiKey: String, context: Any?)`

Initialize the Sonix SDK with your API key. **Required before using any Sonix features.**

**Parameters:**
- `apiKey`: Your API key (starts with `sk_live_` or `sk_test_`)
- `context`: Android Context (required on Android, pass `null` on iOS)

**Example:**
```kotlin
// Android
Sonix.initialize("sk_live_your_api_key_here", context)
```

```swift
// iOS
Sonix.initialize(apiKey: "sk_live_your_api_key_here")
```

#### `initializeLogging()`

Enable debug logging to console. Useful for troubleshooting.

**Example:**
```kotlin
Sonix.initializeLogging()
```

#### `createBufferPool(poolSize: Int, bufferSize: Int): AudioBufferPool`

Create a pre-allocated buffer pool for zero-allocation audio processing.

**Parameters:**
- `poolSize`: Number of buffers to pre-allocate (e.g., 4)
- `bufferSize`: Size of each buffer in samples (e.g., 2048)

**Returns:** `AudioBufferPool` instance

**Example:**
```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

// Use in recording loop
recorder.audioBuffers.collect { buffer ->
    val floatBuffer = pool.acquire()
    val sampleCount = buffer.fillFloatSamples(floatBuffer)
    // Process audio without allocations
    pool.release(floatBuffer)
}
```


### Enums

#### `AudioFormat`

Audio encoding formats.

**Values:**
- `M4A` - AAC encoded in M4A container (recommended)
- `MP3` - MP3 encoded via LAME

#### `Quality`

Predefined recording quality presets.

**Values:**
- `VOICE` - 16kHz mono (smallest files, voice-optimized)
- `STANDARD` - 44.1kHz mono (balanced quality/size)
- `HIGH` - 44.1kHz stereo (best quality, largest files)

---

## SonixRecorder

Audio recording with encoding, level metering, and real-time buffer access.

### Factory Methods

#### `create(outputPath: String, format: String, quality: String): SonixRecorder`

Zero-config factory for recording with quality presets.

**Parameters:**
- `outputPath`: Absolute path for output file
- `format`: `"m4a"` or `"mp3"`
- `quality`: `"voice"`, `"standard"`, or `"high"`

**Returns:** Ready-to-use `SonixRecorder`

**Example:**
```kotlin
val recorder = SonixRecorder.create(
    outputPath = "/path/to/output.m4a",
    format = "m4a",
    quality = "voice"
)
```

```swift
let recorder = SonixRecorder.create(
    outputPath: outputPath,
    format: "m4a",
    quality: "voice"
)
```

### Builder

For advanced configuration with custom sample rates and callbacks.

**Example:**
```kotlin
val recorder = SonixRecorder.Builder()
    .outputPath("/path/to/output.mp3")
    .format("mp3")
    .sampleRate(16000)
    .channels(1)
    .bitrate(128000)
    .onRecordingStarted { println("Started!") }
    .onRecordingStopped { path -> println("Saved: $path") }
    .onError { error -> println("Error: $error") }
    .build()
```

**Builder Methods:**
- `outputPath(String)` - Output file path (required)
- `format(String)` - `"m4a"` or `"mp3"` (default: `"m4a"`)
- `sampleRate(Int)` - Sample rate in Hz (default: 16000)
- `channels(Int)` - 1 = mono, 2 = stereo (default: 1)
- `bitrate(Int)` - Bitrate in bps (default: 128000)
- `onRecordingStarted(() -> Unit)` - Callback when recording starts
- `onRecordingStopped((String) -> Unit)` - Callback with saved file path
- `onError((String) -> Unit)` - Error callback
- `build()` - Create the recorder

### StateFlows

Observable reactive state. Use `collectAsState()` in Compose or `observeX` Swift extensions.

#### `isRecording: StateFlow<Boolean>`

Whether recording is active.

#### `duration: StateFlow<Long>`

Current recording duration in milliseconds.

#### `level: StateFlow<Float>`

Audio level (RMS) from 0.0 (silence) to 1.0 (loud).

#### `error: StateFlow<SonixError?>`

Error state, null if no error.

#### `audioBuffers: SharedFlow<AudioBuffer>`

Raw PCM audio buffers for real-time processing (pitch detection, visualization).

**Example:**
```kotlin
recorder.audioBuffers.collect { buffer ->
    val floats = FloatArray(buffer.sampleCount)
    buffer.fillFloatSamples(floats)
    // Pass to Calibra for pitch detection
    calibra.detectPitch(floats)
}
```

### Methods

#### `start()`

Start recording.

**Example:**
```kotlin
recorder.start()
```

#### `stop()`

Stop recording and save to file.

**Example:**
```kotlin
recorder.stop()
```

#### `release()`

Release resources. Call when done with recorder.

**Example:**
```kotlin
recorder.release()
```

#### `setPlaybackSyncProvider(provider: PlaybackInfoProvider?)`

Sync recording timestamps with backing track playback.

**Parameters:**
- `provider`: Playback info provider (e.g., `SonixPlayer` or `SonixMixer`)

**Example:**
```kotlin
val backing = SonixPlayer.create("/path/to/backing.mp3")
recorder.setPlaybackSyncProvider(backing)
backing.play()
recorder.start()
// Recording timestamps now match backing track time
```

### Properties

#### `actualSampleRate: Int`

Hardware's actual sample rate (read-only). Important on iOS where hardware may differ from requested rate.

#### `bufferPoolAvailable: Int`

Number of available buffers in pool (if using buffer pools).

#### `bufferPoolWasExhausted: Boolean`

Whether buffer pool was exhausted during recording. If true, allocations occurred (defeating zero-allocation purpose).

---

## SonixPlayer

Audio playback with pitch shifting, volume control, looping, and fade effects.

### Factory Methods

#### `create(source: String): SonixPlayer` (suspend/async)

Zero-config factory for playback. Loads file automatically.

**Parameters:**
- `source`: Absolute path to audio file

**Returns:** Ready-to-use `SonixPlayer`

**Example:**
```kotlin
// Android - suspend function
val player = SonixPlayer.create("/path/to/audio.m4a")
```

```swift
// iOS - async function
let player = try await SonixPlayer.create(source: "/path/to/audio.m4a")
```

### Builder

For advanced configuration with callbacks and buffer tuning.

**Example:**
```kotlin
val player = SonixPlayer.Builder()
    .source("/path/to/audio.m4a")
    .volume(0.8f)
    .pitch(-2f)
    .loopCount(3)
    .onPlaybackComplete { println("Finished!") }
    .onError { error -> println("Error: $error") }
    .build()
```

**Builder Methods:**
- `source(String)` - Audio file path (required)
- `volume(Float)` - Initial volume 0.0-1.0 (default: 1.0)
- `pitch(Float)` - Pitch shift in semitones, -12 to +12 (default: 0)
- `loopCount(Int)` - Times to play: 1 = once, -1 = infinite (default: 1)
- `onPlaybackComplete(() -> Unit)` - Callback when playback finishes
- `onLoopComplete((Int, Int) -> Unit)` - Callback with (currentLoop, totalLoops)
- `onError((String) -> Unit)` - Error callback
- `build()` - Create the player

### StateFlows

#### `isPlaying: StateFlow<Boolean>`

Whether playback is active.

#### `currentTime: StateFlow<Long>`

Current playback position in milliseconds.

#### `error: StateFlow<SonixError?>`

Error state, null if no error.

### Methods

#### `play()`

Start or resume playback.

#### `pause()`

Pause playback.

#### `stop()`

Stop playback and reset position to start.

#### `seek(positionMs: Long)`

Seek to position in milliseconds.

**Parameters:**
- `positionMs`: Target position

**Example:**
```kotlin
player.seek(5000)  // Seek to 5 seconds
```

#### `fadeIn(targetVolume: Float, durationMs: Long)` (suspend/async)

Fade volume from 0 to target over duration.

**Parameters:**
- `targetVolume`: Target volume (0.0-1.0)
- `durationMs`: Fade duration in milliseconds

**Example:**
```kotlin
// Kotlin
launch { player.fadeIn(1.0f, 1000) }
```

```swift
// Swift
Task { try await player.fadeIn(targetVolume: 1.0, durationMs: 1000) }
```

#### `fadeOut(durationMs: Long)` (suspend/async)

Fade volume to 0 over duration.

**Parameters:**
- `durationMs`: Fade duration in milliseconds

**Example:**
```kotlin
launch { player.fadeOut(1000) }
```

#### `release()`

Release resources. Call when done with player.

### Properties

#### `duration: Long` (read-only)

Total audio duration in milliseconds.

#### `volume: Float` (mutable)

Current volume (0.0-1.0). Can be changed during playback.

**Example:**
```kotlin
player.volume = 0.5f
```

#### `pitch: Float` (mutable)

Pitch shift in semitones (-12 to +12). Can be changed during playback.

**Example:**
```kotlin
player.pitch = -2f  // Down 2 semitones
```

#### `loopCount: Int` (mutable)

Number of times to play. 1 = once, -1 = infinite. Can be changed during playback.

**Example:**
```kotlin
player.loopCount = -1  // Infinite loop
```

---

## SonixMixer

Multi-track synchronized playback with per-track volume control.

### Factory Methods

#### `create(): SonixMixer`

Zero-config factory for multi-track playback.

**Returns:** Ready-to-use `SonixMixer`

**Example:**
```kotlin
val mixer = SonixMixer.create()
```

### Builder

For advanced configuration with callbacks.

**Example:**
```kotlin
val mixer = SonixMixer.Builder()
    .loopCount(3)
    .onPlaybackComplete { println("All loops finished!") }
    .onLoopComplete { loop -> println("Loop $loop complete") }
    .build()
```

**Builder Methods:**
- `loopCount(Int)` - Times to loop all tracks (default: 1)
- `onPlaybackComplete(() -> Unit)` - Callback when all loops finish
- `onLoopComplete((Int) -> Unit)` - Callback with loop index
- `onError((String) -> Unit)` - Error callback
- `build()` - Create the mixer

### StateFlows

#### `isPlaying: StateFlow<Boolean>`

Whether mixer is playing.

#### `currentTime: StateFlow<Long>`

Current playback position in milliseconds.

#### `error: StateFlow<SonixError?>`

Error state, null if no error.

### Methods

#### `addTrack(name: String, filePath: String): Boolean`

Add track from file. Auto-decodes to PCM.

**Parameters:**
- `name`: Unique track identifier
- `filePath`: Absolute path to audio file

**Returns:** `true` on success

**Example:**
```kotlin
mixer.addTrack("backing", "/path/to/backing.m4a")
mixer.addTrack("vocal", "/path/to/vocal.m4a")
```

#### `addTrack(name: String, data: ByteArray, sampleRate: Int, channels: Int): Boolean`

Add track from raw PCM data.

**Parameters:**
- `name`: Unique track identifier
- `data`: Raw PCM bytes (16-bit signed, little-endian)
- `sampleRate`: Sample rate in Hz
- `channels`: Number of channels (1 = mono, 2 = stereo)

**Returns:** `true` on success

#### `removeTrack(name: String)`

Remove a track.

**Parameters:**
- `name`: Track identifier

#### `hasTrack(name: String): Boolean`

Check if track exists.

**Parameters:**
- `name`: Track identifier

**Returns:** `true` if track exists

#### `getTrackNames(): List<String>`

Get all track names.

**Returns:** List of track identifiers

#### `setTrackVolume(name: String, volume: Float)`

Set track volume immediately.

**Parameters:**
- `name`: Track identifier
- `volume`: Volume 0.0-1.0

**Example:**
```kotlin
mixer.setTrackVolume("backing", 0.8f)
```

#### `fadeTrackVolume(name: String, targetVolume: Float, durationMs: Long)`

Fade track volume from current to target.

**Parameters:**
- `name`: Track identifier
- `targetVolume`: Target volume 0.0-1.0
- `durationMs`: Fade duration in milliseconds

**Example:**
```kotlin
mixer.fadeTrackVolume("vocal", 0.5f, 500)
```

#### `fadeTrackVolume(name: String, startVolume: Float, endVolume: Float, durationMs: Long)`

Fade track volume from start to end.

**Parameters:**
- `name`: Track identifier
- `startVolume`: Start volume 0.0-1.0
- `endVolume`: End volume 0.0-1.0
- `durationMs`: Fade duration in milliseconds

#### `play()`

Start playback of all tracks (synchronized).

#### `pause()`

Pause all tracks.

#### `stop()`

Stop all tracks and reset position.

#### `reset()`

Reset mixer to initial state.

#### `seek(positionMs: Long)`

Seek all tracks to position.

**Parameters:**
- `positionMs`: Target position in milliseconds

#### `release()`

Release resources. Call when done with mixer.

### Properties

#### `duration: Long` (read-only)

Duration of longest track in milliseconds.

#### `loopCount: Int` (mutable)

Number of times to loop. Can be changed during playback.

#### `completedLoops: Int` (read-only)

Number of completed loops so far.

---

## SonixMetronome

Metronome for practice with downbeat and beat samples.

### Factory Methods

#### `create(samaSamplePath: String, beatSamplePath: String, bpm: Float, beatsPerCycle: Int): SonixMetronome`

Zero-config factory for metronome.

**Parameters:**
- `samaSamplePath`: Path to downbeat (first beat) sample
- `beatSamplePath`: Path to regular beat sample
- `bpm`: Beats per minute (30-300)
- `beatsPerCycle`: Beats before repeating (e.g., 4 for 4/4 time)

**Returns:** Ready-to-use `SonixMetronome`

**Example:**
```kotlin
val metronome = SonixMetronome.create(
    samaSamplePath = "/path/to/sama.wav",
    beatSamplePath = "/path/to/beat.wav",
    bpm = 120f,
    beatsPerCycle = 4
)
```

### Builder

For advanced configuration with callbacks.

**Example:**
```kotlin
val metronome = SonixMetronome.Builder()
    .samaSamplePath("/path/to/sama.wav")
    .beatSamplePath("/path/to/beat.wav")
    .bpm(120f)
    .beatsPerCycle(4)
    .volume(0.8f)
    .onBeat { beatIndex -> println("Beat $beatIndex") }
    .build()
```

**Builder Methods:**
- `samaSamplePath(String)` - Downbeat sample path (required)
- `beatSamplePath(String)` - Regular beat sample path (required)
- `bpm(Float)` - Beats per minute (required)
- `beatsPerCycle(Int)` - Beats per cycle (required)
- `volume(Float)` - Initial volume 0.0-1.0 (default: 1.0)
- `onBeat((Int) -> Unit)` - Callback on each beat with beat index
- `build()` - Create the metronome

### StateFlows

#### `isPlaying: StateFlow<Boolean>`

Whether metronome is running.

#### `currentBeat: StateFlow<Int>`

Current beat index (0-based, wraps at beatsPerCycle).

#### `bpm: StateFlow<Float>`

Current BPM.

#### `isInitialized: StateFlow<Boolean>`

Whether samples have been loaded.

#### `error: StateFlow<SonixError?>`

Error state, null if no error.

### Methods

#### `start()`

Start metronome.

#### `stop()`

Stop metronome.

#### `setBpm(bpm: Float)`

Change BPM (takes effect on next beat).

**Parameters:**
- `bpm`: New BPM (30-300)

**Example:**
```kotlin
metronome.setBpm(140f)
```

#### `release()`

Release resources. Call when done with metronome.

### Properties

#### `volume: Float` (mutable)

Current volume (0.0-1.0). Can be changed while running.

**Example:**
```kotlin
metronome.volume = 0.5f
```

#### `beatLengthMs: Int` (read-only)

Duration of each beat in milliseconds (calculated from BPM).

#### `beatsPerCycle: Int` (read-only)

Number of beats per cycle. **Note:** To change this, you must create a new metronome instance.

---

## SonixMidiSynthesizer

MIDI to audio synthesis using SoundFont files.

### Factory Methods

#### `create(soundFontPath: String): SonixMidiSynthesizer`

Zero-config factory for MIDI synthesis.

**Parameters:**
- `soundFontPath`: Path to SoundFont file (.sf2 or .sf3)

**Returns:** Ready-to-use `SonixMidiSynthesizer`

**Example:**
```kotlin
val synth = SonixMidiSynthesizer.create("/path/to/soundfont.sf2")
```

### Builder

For advanced configuration with custom sample rate.

**Example:**
```kotlin
val synth = SonixMidiSynthesizer.Builder()
    .soundFontPath("/path/to/soundfont.sf2")
    .sampleRate(48000)
    .onError { error -> println("Error: $error") }
    .build()
```

**Builder Methods:**
- `soundFontPath(String)` - SoundFont file path (required)
- `sampleRate(Int)` - Output sample rate (default: 44100)
- `onError((String) -> Unit)` - Error callback
- `build()` - Create the synthesizer

### Methods

#### `synthesize(midiPath: String, outputPath: String): Boolean`

Synthesize MIDI file to WAV.

**Parameters:**
- `midiPath`: Path to MIDI file
- `outputPath`: Path for output WAV file

**Returns:** `true` on success

**Example:**
```kotlin
val success = synth.synthesize(
    midiPath = "/path/to/input.mid",
    outputPath = "/path/to/output.wav"
)
```

#### `synthesizeMidi(midiData: ByteArray, outputPath: String): Boolean`

Synthesize raw MIDI bytes to WAV.

**Parameters:**
- `midiData`: Raw MIDI data
- `outputPath`: Path for output WAV file

**Returns:** `true` on success

#### `synthesizeFromNotes(notes: List<MidiNote>, outputPath: String): Boolean`

Synthesize from note list to WAV.

**Parameters:**
- `notes`: List of `MidiNote` objects
- `outputPath`: Path for output WAV file

**Returns:** `true` on success

**Example:**
```kotlin
val notes = listOf(
    MidiNote(note = 60, startTime = 0f, endTime = 500f),     // C4, 0-500ms
    MidiNote(note = 62, startTime = 500f, endTime = 1000f)   // D4, 500-1000ms
)
val success = synth.synthesizeFromNotes(notes, "/path/to/output.wav")
```

#### `synthesizeFromPitchFile(pitchPath: String, outputPath: String, lessonTonicHz: Float, parentTonicHz: Float): Boolean`

Synthesize from pitch contour file to WAV.

**Parameters:**
- `pitchPath`: Path to pitch file (format: `startSec endSec freqHz` per line)
- `outputPath`: Path for output WAV file
- `lessonTonicHz`: Lesson tonic frequency in Hz
- `parentTonicHz`: Parent tonic frequency in Hz

**Returns:** `true` on success

**Example:**
```kotlin
val success = synth.synthesizeFromPitchFile(
    pitchPath = "/path/to/lesson.txt",
    outputPath = "/path/to/output.wav",
    lessonTonicHz = 261.63f,
    parentTonicHz = 261.63f
)
```

#### `release()`

Release resources.

### Properties

#### `soundFontPath: String` (read-only)

Path to loaded SoundFont file.

#### `sampleRate: Int` (read-only)

Output sample rate.

#### `version: String` (read-only)

FluidSynth library version string.

---

## SonixLessonSynthesizer

Synthesize Indian classical music lessons from svara sequences.

### Factory Methods

#### `create(svaras: List<LessonSvara>, beatLengthMs: Int): SonixLessonSynthesizer`

Zero-config factory for lesson synthesis.

**Parameters:**
- `svaras`: List of `LessonSvara` objects
- `beatLengthMs`: Duration of each beat in milliseconds

**Returns:** Ready-to-use `SonixLessonSynthesizer`

**Example:**
```kotlin
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
    )
)

val synth = SonixLessonSynthesizer.create(
    svaras = svaras,
    beatLengthMs = 500
)
```

### Builder

For advanced configuration with silence padding and callbacks.

**Example:**
```kotlin
val synth = SonixLessonSynthesizer.Builder()
    .svaras(svaraList)
    .beatLengthMs(500)
    .silenceBeats(start = 2, end = 2)
    .sampleRate(44100)
    .onError { error -> println("Error: $error") }
    .build()
```

**Builder Methods:**
- `svaras(List<LessonSvara>)` - Svara sequence (required)
- `beatLengthMs(Int)` - Beat duration in ms (required)
- `silenceBeats(Int, Int)` - Silence beats at start and end (default: 2, 2)
- `sampleRate(Int)` - Output sample rate (default: 16000)
- `onError((String) -> Unit)` - Error callback
- `build()` - Create the synthesizer

### StateFlows

#### `isLoading: StateFlow<Boolean>`

Whether audio files are being loaded.

#### `isLoaded: StateFlow<Boolean>`

Whether all audio files have been loaded.

#### `error: StateFlow<SonixError?>`

Error state, null if no error.

### Methods

#### `loadAudio(): Boolean` (suspend)

Load all svara audio files asynchronously.

**Returns:** `true` on success

**Example:**
```kotlin
launch {
    if (synth.loadAudio()) {
        val audioData = synth.synthesize()
    }
}
```

#### `loadAudioSync(): Boolean`

Load all svara audio files synchronously (blocking).

**Returns:** `true` on success

**Example:**
```kotlin
if (synth.loadAudioSync()) {
    val audioData = synth.synthesize()
}
```

#### `synthesize(): AudioRawData?`

Synthesize lesson track from loaded audio.

**Returns:** `AudioRawData` with synthesized audio, or `null` on failure

**Example:**
```kotlin
val audioData = synth.synthesize()
if (audioData != null) {
    // Save to file or play
    SonixEncoder.encode(audioData, "/path/to/lesson.m4a")
}
```

#### `release()`

Release resources.

### Properties

#### `beatLengthMs: Int` (read-only)

Beat duration in milliseconds.

#### `silenceBeatsStart: Int` (read-only)

Number of silence beats at start.

#### `silenceBeatsEnd: Int` (read-only)

Number of silence beats at end.

#### `sampleRate: Int` (read-only)

Output sample rate.

---

## SonixEncoder

Encode raw PCM audio to compressed formats (M4A/AAC, MP3).

### Methods

#### `encode(data: AudioRawData, outputPath: String, format: String, bitrateKbps: Int): Boolean`

Encode `AudioRawData` to file.

**Parameters:**
- `data`: Audio data (e.g., from `SonixDecoder` or synthesis)
- `outputPath`: Output file path
- `format`: `"m4a"` or `"mp3"` (default: `"m4a"`)
- `bitrateKbps`: Target bitrate in kbps (default: 128)

**Returns:** `true` on success

**Example:**
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

#### `encode(samples: FloatArray, sampleRate: Int, channels: Int, outputPath: String, format: String, bitrateKbps: Int): Boolean`

Encode float samples to file.

**Parameters:**
- `samples`: Interleaved float samples in range [-1.0, 1.0]
- `sampleRate`: Sample rate in Hz
- `channels`: Number of channels (1 = mono, 2 = stereo)
- `outputPath`: Output file path
- `format`: `"m4a"` or `"mp3"` (default: `"m4a"`)
- `bitrateKbps`: Target bitrate in kbps (default: 128)

**Returns:** `true` on success

#### `encode(pcmData: ByteArray, sampleRate: Int, channels: Int, outputPath: String, format: String, bitrateKbps: Int): Boolean`

Encode PCM bytes to file.

**Parameters:**
- `pcmData`: 16-bit signed PCM bytes (little-endian, interleaved if stereo)
- `sampleRate`: Sample rate in Hz
- `channels`: Number of channels (1 = mono, 2 = stereo)
- `outputPath`: Output file path
- `format`: `"m4a"` or `"mp3"` (default: `"m4a"`)
- `bitrateKbps`: Target bitrate in kbps (default: 128)

**Returns:** `true` on success

#### `isFormatAvailable(format: String): Boolean`

Check if encoding format is supported on this platform.

**Parameters:**
- `format`: `"m4a"`, `"aac"`, or `"mp3"`

**Returns:** `true` if supported

**Example:**
```kotlin
if (SonixEncoder.isFormatAvailable("mp3")) {
    // Encode to MP3
}
```

---

## SonixDecoder

Decode audio files to raw PCM data.

### Methods

#### `decode(path: String): AudioRawData?`

Decode audio file to PCM.

**Parameters:**
- `path`: Absolute path to audio file

**Returns:** `AudioRawData` with PCM data, or `null` on failure

**Supported formats:** WAV, MP3, M4A, AAC, and other platform-supported formats

**Example:**
```kotlin
val audioData = SonixDecoder.decode("/path/to/audio.mp3")
if (audioData != null) {
    println("Sample rate: ${audioData.sampleRate}")
    println("Channels: ${audioData.numChannels}")
    println("Duration: ${audioData.durationMilliSecs}ms")

    // Use raw PCM data
    val pcmBytes = audioData.audioData
}
```

---

## Data Models

### AudioBuffer

Raw PCM buffer from recording.

**Properties:**
- `data: ByteArray` - Raw PCM bytes (16-bit signed, little-endian)
- `sampleCount: Int` - Number of samples
- `timestamp: Long` - Timestamp in milliseconds
- `durationMs: Long` - Buffer duration in milliseconds

**Methods:**
- `fillFloatSamples(floatArray: FloatArray): Int` - Convert to float samples [-1.0, 1.0], returns sample count
- `floatSamples: FloatArray` - Get float samples (allocates new array)

**Example:**
```kotlin
recorder.audioBuffers.collect { buffer ->
    // Zero-allocation conversion
    val floats = FloatArray(buffer.sampleCount)
    buffer.fillFloatSamples(floats)
    // Process floats...
}
```

### AudioRawData

Decoded audio data.

**Properties:**
- `audioData: ByteArray` - Raw PCM bytes (16-bit signed, little-endian)
- `durationMilliSecs: Long` - Duration in milliseconds
- `numChannels: Int` - Number of channels (1 = mono, 2 = stereo)
- `sampleRate: Int` - Sample rate in Hz

### AudioConfig

Recording configuration.

**Properties:**
- `sampleRate: Int` - Sample rate in Hz
- `channels: Int` - Number of channels (1 = mono, 2 = stereo)
- `bufferSizeMs: Int` - Buffer size in milliseconds

### LessonSvara

Indian classical music svara (note) for lesson synthesis.

**Properties:**
- `svaraName: String` - Full svara name (e.g., "Sa", "Re")
- `svaraLabel: String` - Short label (e.g., "S", "R")
- `svaraAudioFilePath: String` - Path to svara audio file
- `numBeats: Int` - Number of beats this svara spans
- `numSamplesConsonant: Int` - Number of consonant samples for smooth looping
- `audioLengthMilliSecs: Long` - Audio file duration (optional)

### MidiNote

MIDI note for synthesis.

**Properties:**
- `note: Int` - MIDI note number (e.g., 60 = C4)
- `startTime: Float` - Start time in milliseconds
- `endTime: Float` - End time in milliseconds

**Example:**
```kotlin
val note = MidiNote(note = 60, startTime = 0f, endTime = 500f)  // C4 for 500ms
```

### SonixError

Error information.

**Properties:**
- `message: String` - Error message
- `cause: Throwable?` - Underlying exception (optional)

### PlaybackInfoProvider

Interface for playback synchronization. Implemented by `SonixPlayer` and `SonixMixer`.

**Properties:**
- `currentTimeMs: Long` - Current playback time
- `isCurrentlyPlaying: Boolean` - Whether currently playing
- `durationMs: Long` - Total duration

**Custom Implementation Example:**
```kotlin
class CustomPlayerProvider(private val player: ExternalPlayer) : PlaybackInfoProvider {
    override val currentTimeMs: Long get() = player.getCurrentTime()
    override val isCurrentlyPlaying: Boolean get() = player.isPlaying()
    override val durationMs: Long get() = player.getDuration()
}

recorder.setPlaybackSyncProvider(CustomPlayerProvider(externalPlayer))
```

---

## Platform-Specific Notes

### Android

- **Sample Rate:** Always returns requested sample rate
- **Pitch Shifting:** Uses SoundTouch library (JNI)
- **Encoding:** AAC via MediaCodec, MP3 via LAME (JNI)

### iOS

- **Sample Rate:** Hardware may return different rate than requested. Check `actualSampleRate` on recorder.
- **Pitch Shifting:** Uses AVAudioUnitTimePitch
- **Encoding:** AAC via AVFoundation, MP3 via LAME (C interop)
- **Swift Extensions:** Type-safe `observe*` methods for StateFlow observation

**iOS Example:**
```swift
// Type-safe observers (no force casts!)
let task = player.observeIsPlaying { isPlaying in
    self.isPlaying = isPlaying
}
// Cancel when done
task.cancel()
```

---

## Threading & Coroutines

All Sonix APIs are **main-thread safe**. StateFlows update on the main thread.

**Kotlin:**
- File loading: Use `Dispatchers.IO`
- Audio processing: Use `Dispatchers.Default`
- UI updates: Automatic with `collectAsState()` in Compose

**Swift:**
- Factory methods: `async/await` on background
- State updates: Dispatched to `@MainActor` automatically
- Observers: Already main-thread safe

**Example:**
```kotlin
// Kotlin
lifecycleScope.launch {
    val player = SonixPlayer.create("/path/to/audio.m4a")  // IO operation
    player.play()  // Safe to call from main
}

scope.launch {
    player.currentTime.collect { timeMs ->  // Collected on main
        updateProgressBar(timeMs)
    }
}
```

```swift
// Swift
Task {
    let player = try await SonixPlayer.create(source: path)  // Background
    player.play()  // Safe from main
}

playerTask = player.observeCurrentTime { timeMs in  // Main actor
    self.updateProgressBar(timeMs)
}
```

---

## Error Handling

Sonix uses **nullable returns and StateFlows** for errors, not exceptions.

**Check return values:**
```kotlin
val success = recorder.start()
if (!success) {
    // Handle error
}
```

**Observe error StateFlow:**
```kotlin
recorder.error.collect { error ->
    error?.let { err ->
        println("Error: ${err.message}")
    }
}
```

**Use callbacks (Builder):**
```kotlin
val recorder = SonixRecorder.Builder()
    .outputPath(path)
    .onError { error -> showToast("Recording error: $error") }
    .build()
```

---

## SDK Initialization

Sonix requires initialization with an API key before use.

**Required:**
```kotlin
// Android
Sonix.initialize("sk_live_your_api_key_here", context)

// iOS
Sonix.initialize(apiKey: "sk_live_your_api_key_here")
```

All factory methods will throw `SonixNotInitializedException` if called before initialization.
