import SwiftUI
import vozos

/// Advanced Metronome Section using SonixMetronome.Builder().
///
/// Demonstrates the Builder pattern for advanced configuration:
/// - Type-safe observers for UI synchronization
/// - Error handling via observeError
/// - Volume and BPM configuration
///
/// Compare this to MetronomeSectionSimplified.swift which uses SonixMetronome.create().
struct MetronomeSection: View {
    @State private var metronome: SonixMetronome?
    @State private var isRunning = false
    @State private var bpm: Float = 120
    @State private var beatsPerCycle: Int32 = 4
    @State private var currentBeat: Int32 = 0
    @State private var volume: Float = 0.8
    @State private var status = "Ready"
    @State private var isInitialized = false

    // Task handles for cancellation
    @State private var beatTask: Task<Void, Never>?
    @State private var playingTask: Task<Void, Never>?
    @State private var initializedTask: Task<Void, Never>?
    @State private var errorTask: Task<Void, Never>?

    // Store paths for reinitialization
    @State private var samaSamplePath: String?
    @State private var beatSamplePath: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Metronome (Builder API)")
                    .font(.headline)
                Spacer()
                if !isInitialized {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // BPM slider
            HStack {
                Text("BPM:")
                    .frame(width: 40, alignment: .leading)
                Slider(value: Binding(
                    get: { Double(bpm - 60) / 140 },
                    set: { newValue in
                        bpm = Float(60 + newValue * 140)
                        metronome?.setBpm(bpm: bpm)
                    }
                ))
                Text("\(Int(bpm))")
                    .frame(width: 40)
            }

            // Beats per cycle
            HStack {
                Text("Beats:")
                    .frame(width: 50, alignment: .leading)
                beatsButton(count: 3)
                beatsButton(count: 4)
                beatsButton(count: 6)
                beatsButton(count: 8)
            }

            // Volume
            HStack {
                Text("Vol:")
                    .frame(width: 40, alignment: .leading)
                Slider(value: Binding(
                    get: { Double(volume) },
                    set: { newValue in
                        volume = Float(newValue)
                        metronome?.volume = volume
                    }
                ))
                Text("\(Int(volume * 100))%")
                    .frame(width: 40)
            }

            // Beat indicators
            HStack {
                Text("Beat:")
                    .frame(width: 50, alignment: .leading)
                ForEach(0..<Int(beatsPerCycle), id: \.self) { beat in
                    let isCurrentBeat = beat == Int(currentBeat) && isRunning
                    let isSama = beat == 0
                    Circle()
                        .fill(beatColor(isCurrentBeat: isCurrentBeat, isSama: isSama))
                        .frame(width: 24, height: 24)
                }
            }

            // Control buttons
            HStack(spacing: 12) {
                Button("Start") {
                    metronome?.start()
                    status = "Running"
                }
                .disabled(!isInitialized || isRunning)
                .buttonStyle(.borderedProminent)

                Button("Stop") {
                    metronome?.stop()
                    status = "Stopped"
                    currentBeat = 0
                }
                .disabled(!isRunning)
                .buttonStyle(.bordered)
            }
        }
        .task {
            await initializeMetronome()
        }
        .onDisappear {
            // Cancel all observer tasks
            beatTask?.cancel()
            playingTask?.cancel()
            initializedTask?.cancel()
            errorTask?.cancel()
            metronome?.release()
        }
    }

    private func initializeMetronome() async {
        guard let samaPath = copyAssetToFile(name: "sama_click", ext: "wav"),
              let beatPath = copyAssetToFile(name: "beat_click", ext: "wav") else {
            await MainActor.run {
                status = "Click samples not found"
            }
            return
        }

        // Store paths for reinitialization
        samaSamplePath = samaPath
        beatSamplePath = beatPath

        // CREATE METRONOME WITH BUILDER - ADVANCED CONFIGURATION
        let newMetronome = SonixMetronome.Builder()
            .samaSamplePath(path: samaPath)
            .beatSamplePath(path: beatPath)
            .bpm(bpm: bpm)
            .beatsPerCycle(count: beatsPerCycle)
            .volume(volume: volume)
            .build()

        await MainActor.run {
            metronome = newMetronome
            status = "Initializing audio..."

            // Type-safe observers - no force casts needed!
            initializedTask = newMetronome.observeIsInitialized { initialized in
                self.isInitialized = initialized
                if initialized {
                    self.status = "Ready"
                }
            }
            beatTask = newMetronome.observeCurrentBeat { beat in
                self.currentBeat = beat
            }
            playingTask = newMetronome.observeIsPlaying { playing in
                self.isRunning = playing
            }
            errorTask = newMetronome.observeError { error in
                if let err = error {
                    self.status = "Error: \(err.message ?? "Unknown")"
                }
            }
        }
    }

    private func reinitializeMetronome(withBeats newBeats: Int32) {
        guard let samaPath = samaSamplePath,
              let beatPath = beatSamplePath else {
            return
        }

        Task {
            // Cancel existing observers
            beatTask?.cancel()
            playingTask?.cancel()
            initializedTask?.cancel()
            errorTask?.cancel()

            // Release old metronome
            metronome?.release()

            await MainActor.run {
                isInitialized = false
                status = "Reinitializing..."
            }

            // CREATE NEW METRONOME WITH BUILDER - ADVANCED CONFIGURATION
            let newMetronome = SonixMetronome.Builder()
                .samaSamplePath(path: samaPath)
                .beatSamplePath(path: beatPath)
                .bpm(bpm: bpm)
                .beatsPerCycle(count: newBeats)
                .volume(volume: volume)
                .build()

            await MainActor.run {
                metronome = newMetronome
                currentBeat = 0

                // Type-safe observers - no force casts needed!
                initializedTask = newMetronome.observeIsInitialized { initialized in
                    self.isInitialized = initialized
                    if initialized {
                        self.status = "Ready"
                    }
                }
                beatTask = newMetronome.observeCurrentBeat { beat in
                    self.currentBeat = beat
                }
                playingTask = newMetronome.observeIsPlaying { playing in
                    self.isRunning = playing
                }
                errorTask = newMetronome.observeError { error in
                    if let err = error {
                        self.status = "Error: \(err.message ?? "Unknown")"
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func beatsButton(count: Int) -> some View {
        if count == Int(beatsPerCycle) {
            Button("\(count)") {
                if beatsPerCycle != Int32(count) {
                    beatsPerCycle = Int32(count)
                    reinitializeMetronome(withBeats: Int32(count))
                }
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
            .disabled(isRunning)
        } else {
            Button("\(count)") {
                if beatsPerCycle != Int32(count) {
                    beatsPerCycle = Int32(count)
                    reinitializeMetronome(withBeats: Int32(count))
                }
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(isRunning)
        }
    }

    private func beatColor(isCurrentBeat: Bool, isSama: Bool) -> Color {
        if isCurrentBeat && isSama {
            return .red
        } else if isCurrentBeat {
            return .green
        } else if isSama {
            return .red.opacity(0.3)
        } else {
            return .gray.opacity(0.3)
        }
    }
}

#Preview {
    MetronomeSection()
        .padding()
}
