# Sonix iOS Sample App

A SwiftUI sample app demonstrating the Sonix KMP audio library.

## Setup Instructions

### 1. Configure API Key

**IMPORTANT:** The API key must be configured before building the app.

1. Navigate to `SonixSample/SonixSample/`
2. Copy the template file:
   ```bash
   cp Config.swift.template Config.swift
   ```
3. Open `Config.swift` and replace `YOUR_API_KEY_HERE` with your actual API key from https://musicmuni.com/sonix

**Note:** `Config.swift` is gitignored to prevent accidentally committing your API key.

### 2. Build the Sonix XCFramework

Build the Sonix XCFramework from the library:

```bash
cd /path/to/sonix-mobile/library
./gradlew assembleXCFramework
```

The framework will be built to `library/build/XCFrameworks/release/`.

### 3. Create Xcode Project

Since generating Xcode project files programmatically is complex, create the project manually:

1. Open Xcode and create a new project:
   - Choose **iOS > App**
   - Product Name: `SonixSample`
   - Organization: `com.musicmuni.sonix`
   - Interface: **SwiftUI**
   - Language: **Swift**

2. Save the project to `samples/ios/SonixSample/`

3. Add the Sonix framework:
   - Drag `sonix.xcframework` to the project (from `library/build/XCFrameworks/release/`)
   - In project settings, ensure it's added to "Frameworks, Libraries, and Embedded Content"
   - Set "Embed" to "Embed & Sign"

4. Copy the Swift source files from `SonixSample/` folder:
   - `SonixSampleApp.swift`
   - `ContentView.swift`
   - `Sections/*.swift`

5. Add the audio resources from `Resources/`:
   - Add all files to the project's bundle resources

6. Add Info.plist keys:
   - `NSMicrophoneUsageDescription`: "This app needs microphone access to record audio."

### 4. Build and Run

Select a simulator or device and build the project.

## Project Structure

```
SonixSample/
├── SonixSampleApp.swift       # App entry point
├── ContentView.swift          # Main scrollable view
├── Sections/
│   ├── RecordingSection.swift    # Audio recording demo
│   ├── PlaybackSection.swift     # Audio playback demo
│   ├── MultiTrackSection.swift   # Multi-track playback demo
│   ├── MidiSection.swift         # MIDI synthesis demo
│   ├── MetronomeSection.swift    # Metronome demo
│   └── DecodingSection.swift     # Audio decoding demo
├── Resources/
│   ├── sample.m4a
│   ├── vocal.m4a
│   ├── sama_click.wav
│   ├── beat_click.wav
│   └── harmonium.sf2
├── Assets.xcassets/
└── Info.plist
```

## API Usage Notes

The Sonix framework exports Kotlin/Native classes to Swift. Common patterns:

### Creating Instances

```swift
// Players are created via factory functions
let player = IosAudioPlayer()
let multiTrack = MultiTrackPlayerFactoryKt.createMultiTrackPlayer()
let metronome = MetronomePlayerFactoryKt.createMetronomePlayer()
let synthesizer = MidiSynthesizerFactoryKt.createMidiSynthesizer()

// Recording
let config = AudioConfig(sampleRate: 16000, channels: 1, bufferSizeMs: 100)
let recorder = IosAudioRecorder(config: config)
let session = AudioSession(config: config, recorder: recorder, enableEncoding: true, encoderBitRate: 128000)
```

### Observing State

Kotlin StateFlow is exposed as an async sequence in Swift:

```swift
Task {
    for await playing in player.isPlaying {
        await MainActor.run {
            isPlaying = playing as! Bool
        }
    }
}
```

### Async Operations

Kotlin suspend functions become async in Swift:

```swift
let loaded = try await player.load(path: audioPath)
try await session.stopAndSave(outputPath: outputPath)
```

## Troubleshooting

### Build fails with "Cannot find 'Config' in scope"

Make sure you've created `Config.swift` from the template:
```bash
cd SonixSample/SonixSample/
cp Config.swift.template Config.swift
```

Then edit `Config.swift` and add your API key.

### XCFramework not found

Run the build script from the project root:
```bash
cd ../..
./scripts/build.sh ios
```

### "Config.swift" is red/missing in Xcode

1. Right-click on the `SonixSample` folder in Xcode
2. Select "Add Files to SonixSample..."
3. Navigate to and select `Config.swift`
4. Make sure "Copy items if needed" is unchecked
5. Click "Add"

## Requirements

- iOS 16.0+
- Xcode 15.0+
- Swift 5.0
- Sonix XCFramework from library build
