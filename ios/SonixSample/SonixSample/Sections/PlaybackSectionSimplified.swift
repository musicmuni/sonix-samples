import SwiftUI
import vozos

/// Simplified Playback Section using SonixPlayer.create().
///
/// Compare this to PlaybackSection.swift - this version is shorter because
/// player creation and file loading are combined into one step with property-style controls.
struct PlaybackSectionSimplified: View {
    @State private var player: SonixPlayer?
    @State private var isPlaying = false
    @State private var currentTimeMs: Int64 = 0
    @State private var durationMs: Int64 = 0
    @State private var volume: Float = 0.8
    @State private var pitch: Float = 0
    @State private var loopCount: Int32 = 1
    @State private var status = "Loading..."
    @State private var isLoaded = false
    @State private var playingTask: Task<Void, Never>?
    @State private var timeTask: Task<Void, Never>?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Playback (SonixPlayer.create)")
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
                    player?.seek(positionMs: seekPos)
                }
            ))
            .disabled(!isLoaded)

            // Playback controls
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

            // Volume - property-style!
            HStack {
                Text("Volume:")
                    .frame(width: 60, alignment: .leading)
                Slider(value: Binding(
                    get: { Double(volume) },
                    set: { newValue in
                        volume = Float(newValue)
                        player?.volume = volume
                    }
                ))
                Text("\(Int(volume * 100))%")
                    .frame(width: 40)
            }

            // Pitch - only apply on drag end for responsiveness
            HStack {
                Text("Pitch:")
                    .frame(width: 60, alignment: .leading)
                Slider(
                    value: Binding(
                        get: { Double(pitch + 12) / 24 },
                        set: { newValue in
                            pitch = Float(newValue * 24 - 12)
                        }
                    ),
                    onEditingChanged: { editing in
                        if !editing {
                            // Only apply pitch when drag ends
                            player?.pitch = pitch
                        }
                    }
                )
                Text("\(Int(pitch)) st")
                    .frame(width: 40)
            }

            // Loop count - property-style!
            HStack {
                Text("Loop:")
                    .frame(width: 60, alignment: .leading)
                loopButton(count: 1)
                loopButton(count: 2)
                loopButton(count: 3)
                loopButton(count: -1)
            }

            // Fade controls
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
        .task {
            await initializePlayer()
        }
        .onDisappear {
            cancelObservers()
            player?.release()
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

    private func initializePlayer() async {
        // Copy asset to file
        guard let assetPath = copyAssetToFile(name: "sample", ext: "m4a") else {
            await MainActor.run {
                status = "Asset not found"
            }
            return
        }

        do {
            // THIS IS ALL IT TAKES TO CREATE A PLAYER!
            let newPlayer = try await SonixPlayer.create(source: assetPath)

            await MainActor.run {
                player = newPlayer
                durationMs = newPlayer.duration
                isLoaded = true
                status = "Loaded: sample.m4a"
            }

            // Observe playback state
            observePlaybackState(newPlayer)
        } catch {
            await MainActor.run {
                status = "Error: \(error.localizedDescription)"
            }
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

#Preview {
    PlaybackSectionSimplified()
        .padding()
}
