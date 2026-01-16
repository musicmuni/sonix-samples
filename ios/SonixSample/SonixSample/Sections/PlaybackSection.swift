import SwiftUI
import vozos

struct PlaybackSection: View {
    @State private var player: SonixPlayer?
    @State private var isPlaying = false
    @State private var currentTimeMs: Int64 = 0
    @State private var durationMs: Int64 = 0
    @State private var volume: Float = 0.8
    @State private var pitch: Float = 0
    @State private var loopCount: Int32 = 1
    @State private var status = "Ready"
    @State private var isLoaded = false
    @State private var playingTask: Task<Void, Never>?
    @State private var timeTask: Task<Void, Never>?

    // Break out bindings to help Swift type checker
    private var seekBinding: Binding<Double> {
        Binding(
            get: { durationMs > 0 ? Double(currentTimeMs) / Double(durationMs) : 0 },
            set: { newValue in
                let seekPos = Int64(newValue * Double(durationMs))
                player?.seek(positionMs: seekPos)
            }
        )
    }

    private var volumeBinding: Binding<Double> {
        Binding(
            get: { Double(volume) },
            set: { newValue in
                volume = Float(newValue)
                player?.volume = volume
            }
        )
    }

    private var pitchBinding: Binding<Double> {
        Binding(
            get: { Double(pitch + 12) / 24 },
            set: { newValue in
                // Only update the local state - pitch is applied in onEditingChanged
                pitch = Float(newValue * 24 - 12)
            }
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Playback (SonixPlayer.Builder)")
                .font(.headline)

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Time display
            Text("\(formatTime(currentTimeMs)) / \(formatTime(durationMs))")
                .font(.body)

            // Seek slider
            Slider(value: seekBinding)
                .disabled(!isLoaded)

            // Playback controls
            playbackControls

            // Volume control
            volumeControl

            // Pitch control
            pitchControl

            // Loop count
            loopControl

            // Fade controls
            fadeControls
        }
        .task {
            await initializePlayer()
        }
        .onDisappear {
            cancelObservers()
            player?.release()
        }
    }

    private var playbackControls: some View {
        HStack(spacing: 12) {
            Button("Play") {
                player?.play()
            }
            .disabled(!isLoaded || isPlaying)
            .buttonStyle(.borderedProminent)

            Button("Pause") {
                player?.pause()
            }
            .disabled(!isPlaying)
            .buttonStyle(.bordered)

            Button("Stop") {
                player?.stop()
                currentTimeMs = 0
            }
            .disabled(!isLoaded)
            .buttonStyle(.bordered)
        }
    }

    private var volumeControl: some View {
        HStack {
            Text("Volume:")
                .frame(width: 60, alignment: .leading)
            Slider(value: volumeBinding)
            Text("\(Int(volume * 100))%")
                .frame(width: 40)
        }
    }

    private var pitchControl: some View {
        HStack {
            Text("Pitch:")
                .frame(width: 60, alignment: .leading)
            Slider(
                value: pitchBinding,
                onEditingChanged: { editing in
                    if !editing {
                        // Only apply pitch when drag ends for responsiveness
                        player?.pitch = pitch
                    }
                }
            )
            Text("\(Int(pitch)) st")
                .frame(width: 40)
        }
    }

    private var loopControl: some View {
        HStack {
            Text("Loop:")
                .frame(width: 60, alignment: .leading)
            loopButton(count: 1)
            loopButton(count: 2)
            loopButton(count: 3)
            loopButton(count: -1)
        }
    }

    @ViewBuilder
    private func loopButton(count: Int) -> some View {
        let label = count == -1 ? "inf" : "\(count)"
        if count == Int(loopCount) {
            Button(label) {
                loopCount = Int32(count)
                player?.loopCount = loopCount
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
        } else {
            Button(label) {
                loopCount = Int32(count)
                player?.loopCount = loopCount
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private var fadeControls: some View {
        HStack(spacing: 12) {
            Button("Fade In") {
                Task {
                    try? await player?.fadeIn(targetVolume: 1.0, durationMs: 1000)
                }
            }
            .disabled(!isLoaded)
            .buttonStyle(.bordered)
            .controlSize(.small)

            Button("Fade Out") {
                Task {
                    try? await player?.fadeOut(durationMs: 1000)
                }
            }
            .disabled(!isLoaded)
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private func initializePlayer() async {
        do {
            status = "Loading..."

            // Copy asset to file for loading
            guard let assetPath = copyAssetToFile(name: "sample", ext: "m4a") else {
                status = "Asset not found"
                return
            }

            // Create player using Builder pattern with callbacks
            let newPlayer = try await SonixPlayer.Builder()
                .source(path: assetPath)
                .volume(volume: volume)
                .pitch(semitones: pitch)
                .loopCount(count: Int32(loopCount))
                .onComplete {
                    print("Playback completed!")
                }
                .onLoopComplete { loopIndex, totalLoops in
                    print("Loop \(loopIndex) of \(totalLoops) completed")
                }
                .onError { error in
                    print("Playback error: \(error)")
                }
                .build()

            player = newPlayer
            durationMs = newPlayer.duration
            isLoaded = true
            status = "Loaded: sample.m4a"

            // Observe playback state
            observePlaybackState(newPlayer)
        } catch {
            status = "Error: \(error.localizedDescription)"
        }
    }

    private func observePlaybackState(_ player: SonixPlayer) {
        // Use type-safe observers instead of force casts
        playingTask = player.observeIsPlaying { self.isPlaying = $0 }
        timeTask = player.observeCurrentTime { self.currentTimeMs = $0 }
    }

    private func cancelObservers() {
        playingTask?.cancel()
        timeTask?.cancel()
    }

    private func formatTime(_ ms: Int64) -> String {
        let seconds = (ms / 1000) % 60
        let minutes = (ms / 1000) / 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

/// Helper to copy asset to temp file
func copyAssetToFile(name: String, ext: String) -> String? {
    guard let assetURL = Bundle.main.url(forResource: name, withExtension: ext) else {
        return nil
    }
    let tempDir = FileManager.default.temporaryDirectory
    let destURL = tempDir.appendingPathComponent("\(name).\(ext)")

    if !FileManager.default.fileExists(atPath: destURL.path) {
        do {
            try FileManager.default.copyItem(at: assetURL, to: destURL)
        } catch {
            print("Failed to copy asset: \(error)")
            return nil
        }
    }
    return destURL.path
}

#Preview {
    PlaybackSection()
        .padding()
}
