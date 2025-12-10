# Sonix Android Sample App

This sample app demonstrates how to use the Sonix SDK in an Android application.

## Setup

### 1. Install the Sonix SDK

The SDK (AAR file) should be placed in `app/libs/sonix.aar`. This is automatically done by the build script.

### 2. Configure API Key

**IMPORTANT:** The API key must be configured before building the app.

1. Open `local.properties` (or create it if it doesn't exist)
2. Add your Sonix API key:
   ```properties
   sonix.apiKey=YOUR_API_KEY_HERE
   ```

If you don't have `local.properties`, you can copy from the template:
```bash
cp local.properties.template local.properties
```

Then edit `local.properties` and replace `YOUR_API_KEY_HERE` with your actual API key from your account manager at Musicmuni.

**Note:** `local.properties` is gitignored to prevent accidentally committing your API key.

### 3. Build and Run

Open the project in Android Studio and run the app, or use the command line:

```bash
./gradlew assembleDebug
```

## Features Demonstrated

- **Simple API Mode**: High-level `Sonix.*` facade for common use cases
  - Recording audio to M4A/MP3
  - Playing audio files
  - Multi-track audio playback
  - MIDI synthesis
  - Metronome

- **Advanced API Mode**: Low-level APIs for power users
  - Custom recorder configuration (sample rate, channels, bitrate)
  - Buffer pool monitoring
  - Audio decoding and encoding
  - MIDI note parsing

## Troubleshooting

### Build fails with "Sonix API key not found"

Make sure you've added `sonix.apiKey=YOUR_KEY` to `local.properties` in the `samples/android/` directory.

### AAR not found

Run the build script from the project root:
```bash
cd ../..
./scripts/build.sh android
```