import SwiftUI
import sonix

/// Simplified Multi-Track Section using SonixMixer.create().
///
/// Demonstrates the zero-config factory pattern for multi-track playback.
/// Uses type-safe observers (no force casts like `as! Bool`).
/// Compare this to MultiTrackSection.swift which uses SonixMixer.Builder().
struct MultiTrackSectionSimplified: View {
    @State private var mixer: SonixMixer?
    @State private var isPlaying = false
    @State private var currentTimeMs: Int64 = 0
    @State private var durationMs: Int64 = 0
    @State private var backingVolume: Float = 0.8
    @State private var vocalVolume: Float = 1.0
    @State private var status = "Loading..."
    @State private var isLoaded = false

    // Task handles for cancellation
    @State private var playingTask: Task<Void, Never>?
    @State private var timeTask: Task<Void, Never>?
    @State private var errorTask: Task<Void, Never>?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Multi-Track (Simplified API)")
                    .font(.headline)
                Spacer()
                if !isLoaded {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Time display
            Text("\(formatTime(currentTimeMs)) / \(formatTime(durationMs))")
                .font(.body)

            // Seek slider
            Slider(value: Binding(
                get: { durationMs > 0 ? Double(currentTimeMs) / Double(durationMs) : 0 },
                set: { newValue in
                    let seekPos = Int64(newValue * Double(durationMs))
                    mixer?.seek(positionMs: seekPos)
                }
            ))
            .disabled(!isLoaded)

            // Backing volume
            HStack {
                Text("Backing:")
                    .frame(width: 70, alignment: .leading)
                Slider(value: Binding(
                    get: { Double(backingVolume) },
                    set: { newValue in
                        backingVolume = Float(newValue)
                        mixer?.setTrackVolume(name: "backing", volume: backingVolume)
                    }
                ))
                Text("\(Int(backingVolume * 100))%")
                    .frame(width: 40)
            }

            // Vocal volume
            HStack {
                Text("Vocal:")
                    .frame(width: 70, alignment: .leading)
                Slider(value: Binding(
                    get: { Double(vocalVolume) },
                    set: { newValue in
                        vocalVolume = Float(newValue)
                        mixer?.setTrackVolume(name: "vocal", volume: vocalVolume)
                    }
                ))
                Text("\(Int(vocalVolume * 100))%")
                    .frame(width: 40)
            }

            // Playback controls
            HStack(spacing: 12) {
                Button("Play") {
                    mixer?.play()
                }
                .disabled(!isLoaded || isPlaying)
                .buttonStyle(.borderedProminent)

                Button("Pause") {
                    mixer?.pause()
                }
                .disabled(!isPlaying)
                .buttonStyle(.bordered)

                Button("Stop") {
                    mixer?.stop()
                    currentTimeMs = 0
                }
                .disabled(!isLoaded)
                .buttonStyle(.bordered)
            }

            // Fade buttons
            HStack(spacing: 12) {
                Button("Fade Vocal \u{2193}") {
                    mixer?.fadeTrackVolume(name: "vocal", targetVolume: 0.2, durationMs: 500)
                    vocalVolume = 0.2
                }
                .disabled(!isLoaded)
                .buttonStyle(.bordered)
                .controlSize(.small)

                Button("Fade Vocal \u{2191}") {
                    mixer?.fadeTrackVolume(name: "vocal", targetVolume: 1.0, durationMs: 500)
                    vocalVolume = 1.0
                }
                .disabled(!isLoaded)
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .task {
            await initializeMixer()
        }
        .onDisappear {
            // Cancel all observer tasks
            playingTask?.cancel()
            timeTask?.cancel()
            errorTask?.cancel()
            mixer?.release()
        }
    }

    private func initializeMixer() async {
        // Copy assets to files
        guard let backingPath = copyAssetToFile(name: "sample", ext: "m4a"),
              let vocalPath = copyAssetToFile(name: "vocal", ext: "m4a") else {
            await MainActor.run {
                status = "Assets not found"
            }
            return
        }

        // CREATE MIXER - ZERO-CONFIG FACTORY!
        let newMixer = SonixMixer.create()

        // Add tracks - just pass file paths, no manual decoding needed!
        let backingSuccess = try? await newMixer.addTrack(name: "backing", filePath: backingPath)
        let vocalSuccess = try? await newMixer.addTrack(name: "vocal", filePath: vocalPath)

        await MainActor.run {
            mixer = newMixer
            if backingSuccess == true && vocalSuccess == true {
                durationMs = newMixer.duration
                isLoaded = true
                status = "Loaded 2 tracks"

                // Type-safe observers - no force casts needed!
                playingTask = newMixer.observeIsPlaying { playing in
                    self.isPlaying = playing
                }
                timeTask = newMixer.observeCurrentTime { time in
                    self.currentTimeMs = time
                }
                errorTask = newMixer.observeError { error in
                    if let error = error {
                        self.status = "Error: \(error.message)"
                    }
                }
            } else {
                status = "Failed to load tracks"
            }
        }
    }

    private func formatTime(_ ms: Int64) -> String {
        let seconds = (ms / 1000) % 60
        let minutes = (ms / 1000) / 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

#Preview {
    MultiTrackSectionSimplified()
        .padding()
}
