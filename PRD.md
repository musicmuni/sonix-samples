# Sonix Sample App - Product Requirements Document

## Overview

Minimal sample applications for Android and iOS that demonstrate all features of the Sonix audio library. The apps serve as both integration tests and reference implementations for developers.

## Goals

1. Demonstrate all Sonix library features in a working app
2. Provide copy-pasteable code examples
3. Validate the library works end-to-end on real devices
4. Keep the UI minimal - focus on functionality, not aesthetics

## Target Platforms

- **Android**: Kotlin, Jetpack Compose, minSdk 24
- **iOS**: Swift, SwiftUI, iOS 15+

---

## Features to Demonstrate

### 1. Audio Recording

**Basic Recording**
- Start/stop recording with `AudioRecorder`
- Display real-time audio level (amplitude) from `audioStream`
- Show recording duration

**Recording with Encoding**
- Use `AudioSession` with `enableEncoding = true`
- Save recording to M4A file
- Display session state changes (Idle → Recording → Encoding → Finished)
- Play back the saved recording

### 2. Audio Playback

**Single Track Playback**
- Load audio file from assets/bundle
- Play/pause/stop controls
- Seek bar with current position
- Volume slider (0.0 - 1.0)
- Pitch shift slider (-12 to +12 semitones)
- Loop count selector (1, 2, 3, infinite)
- Fade in/out buttons

### 3. Multi-Track Playback

**Synchronized Playback**
- Load 2 tracks: "backing" and "vocal" from assets
- Individual volume sliders per track
- Fade track volume button
- Master play/pause/stop/seek controls
- Show that tracks remain synchronized

### 4. MIDI Synthesis

**Generate Audio from Notes**
- Predefined note sequence (C major scale)
- Synthesize to WAV file using bundled SoundFont
- Play the generated audio
- Show synthesis progress/completion

### 5. Metronome

**Click Track**
- BPM slider (60-200)
- Beats per cycle selector (3, 4, 6, 8)
- Volume slider
- Visual beat indicator (highlights current beat)
- Start/stop button

### 6. Audio Decoding

**Decode and Display Info**
- Decode an audio file from assets
- Display: sample rate, channels, duration, data size

---

## Screen Layout

Single scrollable screen with sections:

```
┌─────────────────────────────────────┐
│  Sonix Sample App                   │
├─────────────────────────────────────┤
│  📍 Recording                       │
│  [Record] [Stop] Duration: 00:00    │
│  Level: ████████░░░░░░░░            │
│  [Save & Play Recording]            │
├─────────────────────────────────────┤
│  🎵 Playback                        │
│  Now: 0:15 / 2:30  [▶️] [⏸️] [⏹️]    │
│  ──●────────────────────── (seek)   │
│  Volume: ──●──────────────          │
│  Pitch:  ────●────────────  0 st    │
│  Loop: [1] [2] [3] [∞]              │
│  [Fade In] [Fade Out]               │
├─────────────────────────────────────┤
│  🎚️ Multi-Track                     │
│  Backing: ──●──────────── 0.8       │
│  Vocal:   ────●────────── 1.0       │
│  [▶️] [⏸️] [⏹️]  0:15 / 2:30         │
├─────────────────────────────────────┤
│  🎹 MIDI Synthesis                  │
│  [Generate C Major Scale]           │
│  Status: Ready / Synthesizing / Done│
│  [Play Generated Audio]             │
├─────────────────────────────────────┤
│  🥁 Metronome                       │
│  BPM: ──────●────────── 120         │
│  Beats: [3] [4] [6] [8]             │
│  [ ● ] [ ○ ] [ ○ ] [ ○ ]  (beat)    │
│  [Start] [Stop]                     │
├─────────────────────────────────────┤
│  📂 Audio Decoding                  │
│  [Decode sample.mp3]                │
│  Sample Rate: 44100 Hz              │
│  Channels: 2                        │
│  Duration: 2500 ms                  │
│  Data Size: 220500 bytes            │
└─────────────────────────────────────┘
```

---

## Assets Required

Bundle these audio files with the sample apps:

| File | Purpose | Format | Duration |
|------|---------|--------|----------|
| `sample.mp3` | Playback demo, decode demo | MP3 | ~30s |
| `backing.wav` | Multi-track backing | WAV | ~30s |
| `vocal.wav` | Multi-track vocal | WAV | ~30s |
| `click_sama.wav` | Metronome downbeat | WAV | ~50ms |
| `click_beat.wav` | Metronome beat | WAV | ~50ms |
| `piano.sf3` | SoundFont for MIDI | SF3 | N/A |

**Note**: Use royalty-free/public domain audio or generate simple tones programmatically.

---

## Technical Requirements

### Android

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **Dependencies**:
  - Sonix AAR (local file)
  - kotlinx-coroutines
  - Napier (logging)

### iOS

- **Language**: Swift
- **UI**: SwiftUI
- **Deployment Target**: iOS 15.0
- **Dependencies**:
  - Sonix XCFramework (embedded)

---

## Project Structure

```
samples/
├── PRD.md                    # This document
├── android/
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── kotlin/.../
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── SampleApp.kt
│   │   │   │   └── ui/
│   │   │   │       ├── RecordingSection.kt
│   │   │   │       ├── PlaybackSection.kt
│   │   │   │       ├── MultiTrackSection.kt
│   │   │   │       ├── MidiSection.kt
│   │   │   │       ├── MetronomeSection.kt
│   │   │   │       └── DecodingSection.kt
│   │   │   ├── assets/
│   │   │   │   ├── sample.mp3
│   │   │   │   ├── backing.wav
│   │   │   │   ├── vocal.wav
│   │   │   │   ├── click_sama.wav
│   │   │   │   ├── click_beat.wav
│   │   │   │   └── piano.sf3
│   │   │   └── res/
│   │   └── libs/
│   │       └── sonix-*.aar
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle/
│
└── ios/
    └── SonixSample/
        ├── SonixSample.xcodeproj
        ├── SonixSample/
        │   ├── SonixSampleApp.swift
        │   ├── ContentView.swift
        │   ├── Views/
        │   │   ├── RecordingSection.swift
        │   │   ├── PlaybackSection.swift
        │   │   ├── MultiTrackSection.swift
        │   │   ├── MidiSection.swift
        │   │   ├── MetronomeSection.swift
        │   │   └── DecodingSection.swift
        │   ├── Resources/
        │   │   ├── sample.mp3
        │   │   ├── backing.wav
        │   │   ├── vocal.wav
        │   │   ├── click_sama.wav
        │   │   ├── click_beat.wav
        │   │   └── piano.sf3
        │   └── Info.plist
        └── Frameworks/
            └── sonix.xcframework
```

---

## Success Criteria

1. **Builds successfully** on both platforms without errors
2. **All features work** on real devices (not just emulator/simulator)
3. **No crashes** during normal usage
4. **Code is readable** and can serve as documentation
5. **Minimal dependencies** - only what's needed for Sonix

---

## Out of Scope

- Beautiful UI/UX design
- Error handling UI (use logs for errors)
- Settings persistence
- Unit tests
- Background audio
- Push notifications
- Analytics

---

## Implementation Order

1. Android sample app (complete and tested)
2. iOS sample app (complete and tested)
3. Update documentation to reference samples
