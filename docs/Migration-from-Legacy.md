# Migration Guide: audioiolib to Sonix

Complete guide for migrating from the legacy `audioiolib` library to the new Sonix KMP library.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Changes](#architecture-changes)
3. [Recording Migration](#recording-migration)
4. [Playback Migration](#playback-migration)
5. [Multi-Track Migration](#multi-track-migration)
6. [MIDI Synthesis Migration](#midi-synthesis-migration)
7. [Evaluation & Analysis Migration](#evaluation--analysis-migration)
8. [Threading & Lifecycle](#threading--lifecycle)
9. [API Mapping Reference](#api-mapping-reference)

---

## Quick Start

### Initialization Required

Sonix requires initialization before use (audioiolib did not):

```kotlin
// Android - in Application.onCreate()
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Sonix.initialize("sk_live_your_api_key_here", this)
    }
}
```

```swift
// iOS - in App init
@main
struct MyApp: App {
    init() {
        Sonix.initialize(apiKey: "sk_live_your_api_key_here")
    }
}
```

### Simplified API (Recommended)

For most apps, use the simplified API - it replaces complex audioiolib patterns with simple factory methods:

**Recording:**

```kotlin
// Legacy: AudioRecorderWithDSP (complex setup)
val recorder = AudioRecorderWithDSP()
recorder.setmMinFreqHzFilter(70f)
recorder.setmDataToBeEncoded(true, outputPath)
recorder.startRecording(true)

// Sonix: One-line factory
val recorder = SonixRecorder.create(
    outputPath = "/path/to/output.m4a",
    format = "m4a",
    quality = "voice"  // 16kHz mono, good for voice
)
recorder.start()
```

**Playback:**

```kotlin
// Legacy: AudioTrackWrapper (manual loading)
val player = AudioTrackWrapper()
player.load(audioData, sampleRate, channels)
player.setmPitchShift(-2f)
player.play()

// Sonix: Async factory with auto-loading
val player = SonixPlayer.create("/path/to/audio.m4a")
player.pitch = -2f
player.play()
```

**Multi-Track:**

```kotlin
// Legacy: Multiple AudioTrackWrapper instances
val backing = AudioTrackWrapper()
val vocal = AudioTrackWrapper()
backing.load(...)
vocal.load(...)
backing.play()
vocal.play()

// Sonix: Single mixer with auto-sync
val mixer = SonixMixer.create()
mixer.addTrack("backing", "/path/to/backing.m4a")
mixer.addTrack("vocal", "/path/to/vocal.m4a")
mixer.play()  // All tracks sync automatically
```

---

## Architecture Changes

### Separation of Concerns

| Concern | Legacy (audioiolib) | New (Sonix + Calibra) |
|---------|--------------------|-----------------------|
| Audio Capture | AudioRecorderWithDSP | Sonix: SonixRecorder |
| Audio Playback | AudioTrackWrapper | Sonix: SonixPlayer |
| Multi-Track | Multiple AudioTrackWrapper | Sonix: SonixMixer |
| Encoding | AACEncoderOld | Sonix: Built-in |
| MIDI Synthesis | GenerateAudioFromMidi | Sonix: SonixMidiSynthesizer |
| Metronome | MetronomeAudioTrack | Sonix: SonixMetronome |
| Pitch Detection | AudioRecorderWithDSP | Calibra: PitchDetector |
| VAD | VoiceActivityDetection | Calibra: VoiceActivityDetector |
| Scoring | EvaluationEngine | Calibra: EvaluationSession |

**Key Change:** Sonix handles audio I/O, Calibra handles audio analysis.

### Threading Model

| Aspect | Legacy | Sonix |
|--------|--------|-------|
| Thread Management | Manual `Executors.newScheduledThreadPool` | Coroutines with structured concurrency |
| Data Access | Polling with `audioFeaturesForNextFrame` | Reactive `StateFlow` collection |
| UI Updates | `runOnUiThread` / `Handler.post` | `withContext(Dispatchers.Main)` |
| Cleanup | Manual `shutdownNow()` | Automatic cancellation via lifecycle |

---

## Recording Migration

### Basic Recording

**Legacy:**

```kotlin
// Monolithic recorder with DSP
val mAudioRecorder = AudioRecorderWithDSP()
mAudioRecorder.setmMinFreqHzFilter(70f)
mAudioRecorder.setmPitchContourGT(referenceNotes)
mAudioRecorder.setmDataToBeEncoded(true, outputPath + Constants.FILE_EXTN_M4A)
mAudioRecorder.setmPlayBackStartTime(SystemClock.uptimeMillis())
mAudioRecorder.startRecording(true)

// Manual polling thread
val executorForData = Executors.newScheduledThreadPool(1)
executorForData.scheduleAtFixedRate({
    val features = mAudioRecorder?.audioFeaturesForNextFrame ?: return@scheduleAtFixedRate
    runOnUiThread {
        updatePitchUI(features.pitch)
    }
}, 0, 10, TimeUnit.MILLISECONDS)

// Stop
mAudioRecorder.stopRecording()
executorForData.shutdownNow()
mAudioRecorder.release()
```

**Sonix (Simplified):**

```kotlin
// Separated recording (Sonix) and analysis (Calibra)
val recorder = SonixRecorder.create(
    outputPath = "/path/to/output.m4a",
    format = "m4a",
    quality = "voice"  // 16kHz mono
)

recorder.start()

// Reactive level updates (replaces polling)
lifecycleScope.launch {
    recorder.level.collect { level ->
        updateLevelMeter(level)
    }
}

// For pitch detection, pass to Calibra
lifecycleScope.launch {
    recorder.audioBuffers.collect { buffer ->
        val floats = buffer.floatSamples
        val pitch = calibra.detectPitch(floats)
        updatePitchUI(pitch)
    }
}

// Stop
recorder.stop()  // Auto-saves to outputPath
recorder.release()
```

### Recording with Custom Config

**Sonix (Advanced):**

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

recorder.start()
```

### Zero-Allocation Buffer Processing

**Legacy:** Hidden inside `AudioRecorderWithDSP`

**Sonix:**

```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

recorder.audioBuffers.collect { buffer ->
    val floats = pool.acquire()
    buffer.fillFloatSamples(floats)
    calibra.detectPitch(floats)
    pool.release(floats)
}
```

### Key Differences

| Aspect | Legacy | Sonix |
|--------|--------|-------|
| Encoding | Hidden setter `setmDataToBeEncoded()` | Explicit in factory: `format = "m4a"` |
| Feature Extraction | Built into recorder | Separate Calibra library |
| Thread Management | Manual `Executors` | Coroutines |
| Data Access | `audioFeaturesForNextFrame` polling | `audioBuffers` Flow |
| UI Updates | `runOnUiThread {}` | `withContext(Dispatchers.Main)` |

---

## Playback Migration

### Basic Playback

**Legacy:**

```kotlin
val mAudioTrackWrapper = AudioTrackWrapper()
mAudioTrackWrapper.load(audioData, sampleRate, channels)
mAudioTrackWrapper.setLoopCount(3)
mAudioTrackWrapper.setmPitchShift(-2f)
mAudioTrackWrapper.setFaderValue(0.8f)
mAudioTrackWrapper.setPlaybackStartPos(5000)
mAudioTrackWrapper.play()

// Manual polling for position
val currentPos = mAudioTrackWrapper.playbackCurrentPos_ms

// Stop
mAudioTrackWrapper.stop()
mAudioTrackWrapper.release()
```

**Sonix (Simplified):**

```kotlin
// Auto-loads from file
val player = SonixPlayer.create("/path/to/audio.m4a")

player.loopCount = 3
player.pitch = -2f
player.volume = 0.8f
player.seek(5000)
player.play()

// Reactive state (replaces polling)
lifecycleScope.launch {
    player.currentTime.collect { timeMs ->
        updateProgressBar(timeMs)
    }
}

// Stop
player.stop()
player.release()
```

### Volume Fading

**Legacy:**

```kotlin
// Simple fade
mAudioTrackWrapper.setVolume(1f, FADE_IN_TIME)

// Complex fade
mAudioTrackWrapper.setVolume(0.0f, 1.0f, TIME, false)
```

**Sonix:**

```kotlin
// Fade in
lifecycleScope.launch {
    player.fadeIn(targetVolume = 1.0f, durationMs = 1000)
}

// Fade out
lifecycleScope.launch {
    player.fadeOut(durationMs = 1000)
}
```

### Key Differences

| Aspect | Legacy | Sonix |
|--------|--------|-------|
| State Access | Polling methods | StateFlow (reactive) |
| Volume | `setFaderValue()` | `volume` property + `fadeIn()` / `fadeOut()` |
| Pitch | `setmPitchShift()` | `pitch` property |
| Seek | `setPlaybackStartPos()` | `seek()` method |
| Position | `playbackCurrentPos_ms` | `currentTime: StateFlow<Long>` |
| Loading | `load(data, sr, ch)` | `create(path)` auto-loads from file |
| Duration | Not available | `duration` property |

---

## Multi-Track Migration

### Basic Multi-Track

**Legacy:**

```kotlin
// Multiple instances with manual sync
val backing = AudioTrackWrapper()
val vocal = AudioTrackWrapper()

backing.load(backingData, sampleRate, channels)
vocal.load(vocalData, sampleRate, channels)

backing.setFaderValue(0.8f)
vocal.setFaderValue(1.0f)

// Manual sync start
backing.play()
vocal.play()
```

**Sonix:**

```kotlin
// Single mixer with built-in sync
val mixer = SonixMixer.create()

mixer.addTrack("backing", "/path/to/backing.m4a")
mixer.addTrack("vocal", "/path/to/vocal.m4a")

mixer.setTrackVolume("backing", 0.8f)
mixer.setTrackVolume("vocal", 1.0f)

// Single play call - all tracks sync automatically
mixer.play()
```

### Volume Control

**Sonix:**

```kotlin
// Immediate volume change
mixer.setTrackVolume("backing", 0.5f)

// Smooth fade
mixer.fadeTrackVolume("vocal", targetVolume = 0.5f, durationMs = 500)

// Custom fade envelope
mixer.fadeTrackVolume("backing", startVolume = 0.8f, endVolume = 0.2f, durationMs = 1000)
```

---

## MIDI Synthesis Migration

### Basic Synthesis

**Legacy:**

```kotlin
val generator = GenerateAudioFromMidi()
generator.generateAudioForLesson(
    context,
    lessonTonicHz,
    parentTonicHz,
    pitchFilePath,
    soundFontPath,
    outputPath
)
```

**Sonix:**

```kotlin
val synthesizer = SonixMidiSynthesizer.create("/path/to/soundfont.sf2")

val success = synthesizer.synthesizeFromPitchFile(
    pitchPath = pitchFilePath,
    outputPath = outputPath,
    lessonTonicHz = lessonTonicHz,
    parentTonicHz = parentTonicHz
)
```

### MIDI from Notes

**Sonix:**

```kotlin
val notes = listOf(
    MidiNote(note = 60, startTime = 0f, endTime = 500f),     // C4
    MidiNote(note = 62, startTime = 500f, endTime = 1000f),  // D4
    MidiNote(note = 64, startTime = 1000f, endTime = 1500f)  // E4
)

val success = synthesizer.synthesizeFromNotes(notes, "/path/to/output.wav")
```

---

## Evaluation & Analysis Migration

### Pitch Detection

**Legacy:**

```kotlin
// Built into AudioRecorderWithDSP
val features = mAudioRecorder.audioFeaturesForNextFrame
val pitch = features.pitch
```

**Sonix + Calibra:**

```kotlin
// Separate Calibra library
val pitchDetector = calibra.createPitchDetector(sampleRate = 16000)

recorder.audioBuffers.collect { buffer ->
    val floats = buffer.floatSamples
    val pitch = pitchDetector.detect(floats)
}
```

### Voice Activity Detection

**Legacy:**

```kotlin
val vadResult = mAudioRecorder.latestVADResult
val ratio = vadResult.vadRatio
val isSinging = ratio > VoiceActivityDetection.PARTIAL_SINGING_THRESHOLD
```

**Sonix + Calibra:**

```kotlin
val vadDetector = calibra.createVADDetector()

recorder.audioBuffers.collect { buffer ->
    val floats = buffer.floatSamples
    val isSinging = vadDetector.detect(floats)
}
```

### Scoring & Evaluation

**Legacy:**

```kotlin
// Initialize evaluation
mAudioRecorder.initSingingEvaluation(referenceFilePath)

// Real-time scoring
val score = mAudioRecorder.computeCrudeScore(pitch, timestamp)
val correctRatio = mAudioRecorder.pitchCorrectFrameRatio

// Post-recording feedback
val feedback = EvaluationEngine.generateFeedbackEvaluateFlatSvars(
    recordedPitch, referencePitch, timestamps, ...
)
```

**Sonix + Calibra:**

```kotlin
// Initialize evaluation session
val session = calibra.createEvaluationSession(
    referenceFilePath = referenceFilePath,
    minFreqHz = 70f
)

// Real-time scoring
recorder.audioBuffers.collect { buffer ->
    val floats = buffer.floatSamples
    val score = session.evaluateFrame(floats)
}

// Post-recording feedback
val feedback = session.generateFeedback()
```

### Vocal Range Analysis

**Legacy:**

```kotlin
val histogram = EvaluationEngine.getUpdatedPitchHistogramForUser(pitchArray, ...)
val (minMidi, maxMidi) = EvaluationEngine.getUserVocalRangeMidiFromHistogram(histogram)
```

**Sonix + Calibra:**

```kotlin
val analyzer = calibra.createVocalRangeAnalyzer()
val range = analyzer.analyze(pitchArray)
println("Range: ${range.minNote} to ${range.maxNote}")
```

### Breath Capacity

**Legacy:**

```kotlin
val breathCapacity = EvaluationEngine.getBreathCapacityForLesson(
    pitchArray, timestamps, ...
)
```

**Sonix + Calibra:**

```kotlin
val breathAnalyzer = calibra.createBreathAnalyzer()
val capacity = breathAnalyzer.analyze(pitchArray, timestamps)
```

---

## Threading & Lifecycle

### Threading Model

**Legacy:**

```kotlin
// Manual executor management
var executorForData: ScheduledExecutorService? = null

fun startRecording() {
    mAudioRecorder.startRecording(true)

    executorForData = Executors.newScheduledThreadPool(1)
    executorForData?.scheduleAtFixedRate({
        val features = mAudioRecorder?.audioFeaturesForNextFrame ?: return@scheduleAtFixedRate
        runOnUiThread {
            updateUI(features)
        }
    }, 0, 10, TimeUnit.MILLISECONDS)
}

fun stopRecording() {
    mAudioRecorder?.stopRecording()
    executorForData?.shutdownNow()
}
```

**Sonix:**

```kotlin
// Coroutines with lifecycle scope
fun startRecording() {
    recorder.start()

    lifecycleScope.launch {
        recorder.audioBuffers.collect { buffer ->
            // Process on background thread
            val pitch = calibra.detectPitch(buffer.floatSamples)

            // Update UI on main thread
            withContext(Dispatchers.Main) {
                updateUI(pitch)
            }
        }
    }
}

fun stopRecording() {
    recorder.stop()
    // Coroutines automatically cancelled when lifecycle stops
}
```

### Lifecycle Management

**Legacy:**

```kotlin
// Nullable with manual null checks
var mAudioRecorder: AudioRecorderWithDSP? = null
var mAudioTrackWrapper: AudioTrackWrapper? = null

override fun onPause() {
    super.onPause()
    mAudioRecorder?.release()
    mAudioTrackWrapper?.release()
    executorForData?.shutdownNow()
}

// In loops
if (mAudioRecorder == null) return
```

**Sonix:**

```kotlin
// Non-null with lateinit
private lateinit var recorder: SonixRecorder
private val player by lazy { SonixPlayer.create("/path/to/audio.m4a") }

// Automatic cancellation via lifecycle scope
lifecycleScope.launchWhenStarted {
    recorder.level.collect { level ->
        // Automatically cancelled when lifecycle stops
    }
}

override fun onDestroy() {
    super.onDestroy()
    recorder.release()
    player.release()
}
```

---

## API Mapping Reference

### AudioRecorderWithDSP → SonixRecorder

| Legacy | Sonix | Notes |
|--------|-------|-------|
| `AudioRecorderWithDSP()` | `SonixRecorder.create(...)` | Factory method |
| `setmMinFreqHzFilter()` | Moved to Calibra | Filter in PitchDetector |
| `setmPitchContourGT()` | Moved to Calibra | Reference in EvaluationSession |
| `setmDataToBeEncoded()` | `format` parameter | Explicit in factory |
| `startRecording(true)` | `start()` | Simplified |
| `stopRecording()` | `stop()` | Auto-saves |
| `audioFeaturesForNextFrame` | `audioBuffers` Flow | Reactive |
| `latestVADResult` | Moved to Calibra | VAD in VoiceActivityDetector |
| `pitchCorrectFrameRatio` | Moved to Calibra | Scoring in EvaluationSession |
| `recordedDuration_ms` | `duration` StateFlow | Reactive |
| `release()` | `release()` | Same |

### AudioTrackWrapper → SonixPlayer

| Legacy | Sonix | Notes |
|--------|-------|-------|
| `AudioTrackWrapper()` | `SonixPlayer.create(path)` | Auto-loads from file |
| `load(data, sr, ch)` | Built into `create()` | Async loading |
| `setmPitchShift()` | `pitch` property | Mutable property |
| `setFaderValue()` | `volume` property | Mutable property |
| `setLoopCount()` | `loopCount` property | Mutable property |
| `setPlaybackStartPos()` | `seek()` | Method call |
| `playbackCurrentPos_ms` | `currentTime` StateFlow | Reactive |
| `play() / pause() / stop()` | `play() / pause() / stop()` | Same |
| `release()` | `release()` | Same |

### EvaluationEngine → Calibra

| Legacy | Calibra | Notes |
|--------|---------|-------|
| `pitchYIN()` | `PitchDetector.detect()` | Separate library |
| `getAmplitude()` | `AmplitudeAnalyzer` | Separate component |
| `getVADRatio()` | `VoiceActivityDetector` | Separate component |
| `computeCrudeScore()` | `EvaluationSession.evaluate()` | Real-time scoring |
| `generateFeedback...()` | `EvaluationSession.generateFeedback()` | Post-recording |
| `getUserVocalRangeMidi...()` | `VocalRangeAnalyzer` | Separate component |
| `computeSpokenShrutiHertz()` | `ShrutiAnalyzer` | Separate component |

---

## Summary

### Migration Checklist

- [ ] Add `Sonix.initialize()` call in app startup
- [ ] Replace `AudioRecorderWithDSP` with `SonixRecorder.create()`
- [ ] Replace `AudioTrackWrapper` with `SonixPlayer.create()`
- [ ] Replace polling loops with Flow collection
- [ ] Replace `runOnUiThread` with `withContext(Dispatchers.Main)`
- [ ] Replace manual executors with lifecycle coroutines
- [ ] Move DSP code to Calibra library
- [ ] Update cleanup to use `release()` in `onDestroy()`
- [ ] Test on both Android and iOS

### Key Benefits

| Benefit | Description |
|---------|-------------|
| **Simpler API** | One-line factories replace complex setup |
| **Reactive State** | StateFlows replace manual polling |
| **Type Safety** | Strong typing, no hidden setters |
| **Cross-Platform** | Single codebase for Android & iOS |
| **Separation of Concerns** | Audio I/O (Sonix) vs Analysis (Calibra) |
| **Modern Threading** | Coroutines replace manual executors |
| **Better Lifecycle** | Automatic cancellation via lifecycle scopes |

### Need Help?

- **API Reference**: See `API-Reference.md` for detailed method docs
- **Advanced Topics**: See `Advanced-Topics.md` for threading, performance, platform differences
- **Troubleshooting**: See Advanced Topics > Troubleshooting section
