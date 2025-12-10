# Advanced Topics

Deep dive into Sonix internals, performance optimization, platform differences, and advanced integration patterns.

---

## Table of Contents

1. [SDK Initialization & Licensing](#sdk-initialization--licensing)
2. [Threading Model](#threading-model)
3. [Performance Optimization](#performance-optimization)
4. [Platform Differences](#platform-differences)
5. [Custom Integration](#custom-integration)
6. [Troubleshooting](#troubleshooting)

---

## SDK Initialization & Licensing

### Required Initialization

Sonix requires initialization before any SDK features can be used:

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
// iOS - in App init or AppDelegate
@main
struct MyApp: App {
    init() {
        Sonix.initialize(apiKey: "sk_live_your_api_key_here")
    }
}
```

All factory methods (`SonixRecorder.create()`, `SonixPlayer.create()`, etc.) will throw `SonixNotInitializedException` if called before initialization.

### API Keys

- **Live keys** start with `sk_live_` - for production
- **Test keys** start with `sk_test_` - for development

Store keys securely:

```kotlin
// DON'T hardcode in source (visible in APK/IPA)
Sonix.initialize("sk_live_abc123...", context)

// DO use build config or secure storage
Sonix.initialize(BuildConfig.SONIX_API_KEY, context)
```

---

## Threading Model

Sonix uses Kotlin coroutines for all asynchronous operations. All public APIs are main-thread safe.

### Dispatchers

Internally, Sonix uses three dispatchers:

| Dispatcher | Used For | Examples |
|------------|----------|----------|
| `Dispatchers.Main` | StateFlow updates, lifecycle events | `isPlaying`, `currentTime`, `level` |
| `Dispatchers.IO` | File I/O, network, encoding/decoding | `load()`, `decode()`, `encode()` |
| `Dispatchers.Default` | Audio processing, buffer management | Recording callbacks, mixing |

### StateFlow Updates

All state changes are published on the main thread:

```kotlin
// Automatic main-thread updates in Compose
val isPlaying by player.isPlaying.collectAsState()

// Manual collection (already on main thread)
lifecycleScope.launch {
    player.currentTime.collect { timeMs ->
        updateProgressBar(timeMs)  // Safe to update UI directly
    }
}
```

### Audio Callbacks

Audio buffers arrive on background threads:

```kotlin
recorder.audioBuffers.collect { buffer ->
    // ⚠️ This runs on Dispatchers.Default
    // Don't do UI updates here!

    // Process audio (safe)
    val floats = buffer.floatSamples
    calibra.detectPitch(floats)

    // Update UI (must switch to main thread)
    withContext(Dispatchers.Main) {
        updatePitchDisplay(pitch)
    }
}
```

### Structured Concurrency

Use lifecycle-aware scopes to avoid leaks:

```kotlin
class MyActivity : ComponentActivity() {
    private lateinit var recorder: SonixRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorder = SonixRecorder.create(...)

        // Automatically cancelled when lifecycle stops
        lifecycleScope.launch {
            recorder.level.collect { level ->
                updateLevelMeter(level)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.release()  // Always release resources
    }
}
```

### Blocking Operations

Some operations are blocking (suspend functions):

```kotlin
// Loading files - runs on Dispatchers.IO internally
lifecycleScope.launch {
    val player = SonixPlayer.create("/path/to/audio.m4a")  // Suspend
    player.play()
}

// Encoding - blocking, run on background
lifecycleScope.launch(Dispatchers.IO) {
    val success = SonixEncoder.encode(audioData, outputPath, "m4a", 128)
}
```

---

## Performance Optimization

### Buffer Pools

For real-time DSP (pitch detection, visualization), use pre-allocated buffer pools to avoid garbage collection:

**Without Pool (allocates on every callback):**

```kotlin
recorder.audioBuffers.collect { buffer ->
    val floats = buffer.floatSamples  // ❌ Allocates FloatArray on every call
    calibra.detectPitch(floats)
}
```

**With Pool (zero allocation):**

```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

recorder.audioBuffers.collect { buffer ->
    val floats = pool.acquire()  // ✅ Reuses pre-allocated buffer
    buffer.fillFloatSamples(floats)
    calibra.detectPitch(floats)
    pool.release(floats)
}
```

### Detecting Pool Exhaustion

Monitor pool health to tune `poolSize`:

```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

// After recording session
if (pool.wasExhausted) {
    Log.w("Pool exhausted - increase poolSize to ${pool.totalAllocated}")
}

// Or check on recorder
if (recorder.bufferPoolWasExhausted) {
    Log.w("Recorder pool exhausted!")
}
```

### Optimal Pool Size

| Use Case | Recommended Pool Size |
|----------|----------------------|
| Single recorder + pitch detection | 4 |
| Recorder + multiple DSP pipelines | 8 |
| Recorder + visualization + analysis | 8-12 |

### Zero-Allocation Patterns

**Good (zero allocation):**

```kotlin
val floats = pool.acquire()
buffer.fillFloatSamples(floats)
// Process...
pool.release(floats)
```

**Bad (allocates every time):**

```kotlin
val floats = buffer.floatSamples  // Creates new FloatArray
// Process...
```

### Memory Management

**Release resources promptly:**

```kotlin
override fun onDestroy() {
    super.onDestroy()
    recorder.release()
    player.release()
    mixer.release()
    metronome.release()
}
```

**Cancel coroutine jobs:**

```kotlin
class MyViewModel : ViewModel() {
    private var recordingJob: Job? = null

    fun startRecording() {
        recordingJob = viewModelScope.launch {
            recorder.audioBuffers.collect { buffer ->
                // Process...
            }
        }
    }

    override fun onCleared() {
        recordingJob?.cancel()
        recorder.release()
    }
}
```

### Latency Optimization

**Reduce buffer size** for lower latency (may increase CPU usage):

```kotlin
val config = AudioConfig(
    sampleRate = 16000,
    channels = 1,
    bufferSizeMs = 10  // Default is 20ms, reduce to 10ms for lower latency
)
```

**Trade-offs:**
- Smaller buffers = lower latency, higher CPU usage
- Larger buffers = higher latency, lower CPU usage
- Sweet spot: 10-20ms for real-time feedback

---

## Platform Differences

### Android

**Sample Rate:**
- Always returns requested sample rate
- Hardware resamples if needed
- No surprises

**Pitch Shifting:**
- Uses SoundTouch library (JNI)
- High quality, low latency
- Real-time changes supported

**Encoding:**
- AAC: MediaCodec (hardware accelerated)
- MP3: LAME (software, via JNI)
- Both are efficient

**Threading:**
- Audio callbacks: Custom audio thread
- StateFlows: Main looper
- File I/O: IO executor

### iOS

**Sample Rate:**
- Hardware may override requested rate
- Check `actualSampleRate` on recorder:

```kotlin
val recorder = SonixRecorder.create(...)
println("Requested: 16000 Hz")
println("Actual: ${recorder.actualSampleRate} Hz")  // May differ!

// Use actual rate for encoding
SonixEncoder.encode(
    data = audioData,
    sampleRate = recorder.actualSampleRate,  // Use this!
    channels = 1,
    outputPath = path
)
```

**Common iOS Hardware Rates:**
- iPhone: 48000 Hz (most common)
- iPad: 44100 Hz or 48000 Hz
- Older devices: 44100 Hz

**Pitch Shifting:**
- Uses AVAudioUnitTimePitch
- Excellent quality
- Real-time changes supported
- Slightly different sound than Android (different algorithm)

**Encoding:**
- AAC: AVFoundation (hardware accelerated)
- MP3: LAME (software, via C interop)
- Both perform well

**Threading:**
- Audio callbacks: AVAudioEngine's internal thread
- StateFlows: Main actor
- File I/O: Background dispatch queue

### Cross-Platform Best Practices

**Always use `actualSampleRate`:**

```kotlin
// ✅ Correct (cross-platform)
val recorder = SonixRecorder.create(...)
calibra.initialize(sampleRate = recorder.actualSampleRate)

// ❌ Wrong (breaks on iOS)
calibra.initialize(sampleRate = 16000)  // May be 48000 on iOS!
```

**Test pitch shifting on both platforms:**

```kotlin
// Same code, slightly different sound due to algorithm differences
player.pitch = -2f  // Down 2 semitones
```

**Encoding format compatibility:**

| Format | Android | iOS | Compatibility |
|--------|---------|-----|--------------|
| M4A (AAC) | ✅ | ✅ | Universal |
| MP3 | ✅ | ✅ | Universal |

Both formats work identically on both platforms.

---

## Custom Integration

### PlaybackInfoProvider

Sync recording with external playback sources (YouTube player, streaming service, etc.):

**Interface:**

```kotlin
interface PlaybackInfoProvider {
    val currentTimeMs: Long
    val isCurrentlyPlaying: Boolean
    val durationMs: Long
}
```

**Built-in Providers:**
- `SonixPlayer` implements `PlaybackInfoProvider`
- `SonixMixer` implements `PlaybackInfoProvider`

**Custom Implementation:**

```kotlin
// Example: YouTube player
class YouTubePlayerProvider(private val player: YouTubePlayer) : PlaybackInfoProvider {
    override val currentTimeMs: Long
        get() = player.currentTimeMillis.toLong()

    override val isCurrentlyPlaying: Boolean
        get() = player.isPlaying

    override val durationMs: Long
        get() = player.durationMillis.toLong()
}

// Use with recorder
val backing = YouTubePlayerProvider(youtubePlayer)
recorder.setPlaybackSyncProvider(backing)

youtubePlayer.play()
recorder.start()
// Recording timestamps now match YouTube playback time
```

**Use Cases:**
- Karaoke apps (sync with video playback)
- Music learning apps (practice over backing tracks)
- Sing-along features (record with reference audio)

### Polling vs Reactive

Sonix offers both patterns:

**Reactive (Recommended):**

```kotlin
// StateFlows - automatic updates
recorder.level.collect { level ->
    updateLevelMeter(level)
}

player.currentTime.collect { timeMs ->
    updateProgressBar(timeMs)
}
```

**Polling (Legacy compatibility):**

```kotlin
// Manual polling (if you need it)
val currentTime = player.currentTime.value
val isPlaying = player.isPlaying.value

// Or via PlaybackInfoProvider
val provider: PlaybackInfoProvider = player
val timeMs = provider.currentTimeMs
```

**When to use polling:**
- Legacy code migration
- Custom polling loops
- External libraries that expect polling

**When to use reactive:**
- New code (recommended)
- Compose UI (automatic updates)
- Better performance (only updates when changed)

---

## Troubleshooting

### Permissions

**Android:**

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

```kotlin
// Request at runtime (Android 6+)
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        REQUEST_CODE)
}
```

**iOS:**

```xml
<!-- Info.plist -->
<key>NSMicrophoneUsageDescription</key>
<string>We need microphone access for recording</string>
```

### Common Errors

#### "SonixNotInitializedException"

**Cause:** Forgot to call `Sonix.initialize()`

**Fix:**

```kotlin
// Android - in Application.onCreate()
Sonix.initialize("sk_live_...", this)

// iOS - in App init
Sonix.initialize(apiKey: "sk_live_...")
```

#### "Failed to start recording"

**Possible causes:**
1. No microphone permission
2. Another app is using microphone
3. Hardware issue

**Debug:**

```kotlin
try {
    recorder.start()
} catch (e: Exception) {
    Log.e("Recording", "Failed: ${e.message}", e)
}

// Or use error StateFlow
recorder.error.collect { error ->
    error?.let { Log.e("Recording error: ${it.message}") }
}
```

#### "Sample rate mismatch" (iOS)

**Cause:** Using requested rate instead of actual hardware rate

**Fix:**

```kotlin
// ❌ Wrong
calibra.initialize(sampleRate = 16000)

// ✅ Correct
calibra.initialize(sampleRate = recorder.actualSampleRate)
```

#### "Buffer pool exhausted"

**Cause:** Pool size too small for processing rate

**Fix:**

```kotlin
// Increase pool size
val pool = Sonix.createBufferPool(poolSize = 8, bufferSize = 2048)

// Or check exhaustion and tune
if (pool.wasExhausted) {
    // Recreate with larger size
}
```

#### "Encoding failed"

**Possible causes:**
1. Invalid output path (directory doesn't exist)
2. No write permission
3. Unsupported format/bitrate combination

**Debug:**

```kotlin
val success = SonixEncoder.encode(data, outputPath, "m4a", 128)
if (!success) {
    // Check path
    val dir = File(outputPath).parentFile
    if (!dir.exists()) {
        Log.e("Directory doesn't exist: $dir")
    }

    // Check format
    if (!SonixEncoder.isFormatAvailable("mp3")) {
        Log.e("MP3 encoding not available")
    }
}
```

### Debugging Tips

**Enable logging:**

```kotlin
Sonix.initializeLogging()
// Now all Sonix operations log to console
```

**Monitor StateFlows:**

```kotlin
lifecycleScope.launch {
    recorder.error.collect { error ->
        error?.let { Log.e("Recorder error: ${it.message}") }
    }
}

lifecycleScope.launch {
    player.error.collect { error ->
        error?.let { Log.e("Player error: ${it.message}") }
    }
}
```

**Check buffer pool health:**

```kotlin
val pool = Sonix.createBufferPool(poolSize = 4, bufferSize = 2048)

// After use
Log.d("Pool available: ${pool.available}/${pool.totalAllocated}")
if (pool.wasExhausted) {
    Log.w("Pool exhausted - tune poolSize!")
}
```

**Verify file output:**

```kotlin
recorder.stop()
val file = File(outputPath)
if (file.exists()) {
    Log.d("Saved: ${file.length()} bytes")
} else {
    Log.e("File not saved!")
}
```

### Performance Profiling

**Android Studio Profiler:**
- Monitor memory for buffer allocations
- Check CPU usage during real-time processing
- Watch for GC pauses

**Xcode Instruments:**
- Time Profiler: Check audio thread performance
- Allocations: Verify zero-allocation with buffer pools
- System Trace: View thread activity

**Expected Performance:**
- Recording: <5% CPU (16kHz mono)
- Playback: <10% CPU (44.1kHz stereo with pitch shifting)
- Zero allocations in audio callbacks (with buffer pools)

### Getting Help

1. **Check logs**: Enable `Sonix.initializeLogging()`
2. **Monitor errors**: Collect from `error` StateFlows
3. **Verify initialization**: Ensure `Sonix.initialize()` was called
4. **Test permissions**: Check microphone access
5. **Platform-specific**: Use `actualSampleRate` on iOS
6. **Documentation**: Refer to API Reference for detailed method docs

---

## Best Practices Summary

1. **Always initialize SDK** before using any features
2. **Use buffer pools** for real-time DSP to avoid allocations
3. **Check `actualSampleRate`** on iOS for encoding
4. **Release resources** in `onDestroy()` / `deinit`
5. **Use lifecycle scopes** to avoid coroutine leaks
6. **Monitor error StateFlows** for debugging
7. **Enable logging** during development
8. **Test on both platforms** for pitch shifting differences
9. **Use reactive StateFlows** instead of polling when possible
10. **Store API keys securely** (not in source code)
