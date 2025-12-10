import SwiftUI
import sonix

/// MIDI Synthesis Section using SonixMidiSynthesizer.Builder.
///
/// Demonstrates SonixMidiSynthesizer.Builder() for advanced configuration:
/// - Custom sample rate (44100, 48000)
/// - Error callback
/// - synthesizeFromNotes() for programmatic MIDI generation
/// - SonixPlayer for playback of synthesized audio
struct MidiSection: View {
    @State private var status = "Ready"
    @State private var isSynthesizing = false
    @State private var outputFile: URL?
    @State private var player: SonixPlayer?
    @State private var isPlaying = false
    @State private var synthesizerVersion: String?

    // Advanced options
    @State private var selectedSampleRate: Int32 = 44100

    // Observer task for cancellation
    @State private var playingTask: Task<Void, Never>?

    private let sampleRates: [Int32] = [44100, 48000]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("MIDI Synthesis (SonixMidiSynthesizer.Builder)")
                .font(.headline)

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Show FluidSynth version if available
            if let version = synthesizerVersion {
                Text("FluidSynth: \(version)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // C major scale notes
            Text("Notes: C4 - D4 - E4 - F4 - G4 - A4 - B4 - C5")
                .font(.caption)
                .foregroundColor(.secondary)

            // Sample rate selection
            HStack {
                Text("Sample Rate:")
                    .font(.caption)
                ForEach(sampleRates, id: \.self) { rate in
                    sampleRateButton(rate: rate)
                }
            }

            // Synthesize button
            Button(isSynthesizing ? "Synthesizing..." : "Synthesize") {
                synthesize()
            }
            .disabled(isSynthesizing)
            .buttonStyle(.borderedProminent)

            // Playback controls
            HStack(spacing: 12) {
                Button("Play") {
                    player?.play()
                }
                .disabled(outputFile == nil || isSynthesizing || isPlaying)
                .buttonStyle(.bordered)

                Button("Stop") {
                    player?.stop()
                }
                .disabled(!isPlaying)
                .buttonStyle(.bordered)
            }
        }
        .onDisappear {
            playingTask?.cancel()
            player?.release()
        }
    }

    @ViewBuilder
    private func sampleRateButton(rate: Int32) -> some View {
        let label = "\(rate / 1000)kHz"
        if selectedSampleRate == rate {
            Button(label) {
                selectedSampleRate = rate
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
            .disabled(isSynthesizing)
        } else {
            Button(label) {
                selectedSampleRate = rate
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(isSynthesizing)
        }
    }

    private func synthesize() {
        Task {
            isSynthesizing = true
            status = "Synthesizing (\(selectedSampleRate / 1000)kHz)..."

            do {
                // Copy soundfont to file
                guard let sfPath = copyAssetToFile(name: "harmonium", ext: "sf2") else {
                    status = "Soundfont not found"
                    isSynthesizing = false
                    return
                }

                // C major scale (timing in milliseconds)
                let notes: [MidiNote] = [
                    MidiNote(note: 60, startTime: 0, endTime: 400),     // C4
                    MidiNote(note: 62, startTime: 500, endTime: 900),   // D4
                    MidiNote(note: 64, startTime: 1000, endTime: 1400), // E4
                    MidiNote(note: 65, startTime: 1500, endTime: 1900), // F4
                    MidiNote(note: 67, startTime: 2000, endTime: 2400), // G4
                    MidiNote(note: 69, startTime: 2500, endTime: 2900), // A4
                    MidiNote(note: 71, startTime: 3000, endTime: 3400), // B4
                    MidiNote(note: 72, startTime: 3500, endTime: 4400), // C5
                ]

                let tempDir = FileManager.default.temporaryDirectory
                let outFile = tempDir.appendingPathComponent("midi_output.wav")

                // BUILDER API: Advanced configuration
                let synth = SonixMidiSynthesizer.Builder()
                    .soundFontPath(path: sfPath)
                    .sampleRate(rate: selectedSampleRate)
                    .build()

                synthesizerVersion = synth.version

                let success = synth.synthesizeFromNotes(
                    notes: notes,
                    outputPath: outFile.path
                )

                if success {
                    let attrs = try FileManager.default.attributesOfItem(atPath: outFile.path)
                    let fileSize = (attrs[.size] as? Int64) ?? 0
                    outputFile = outFile
                    status = "Generated (\(selectedSampleRate / 1000)kHz): \(fileSize / 1024) KB"

                    // Load into SonixPlayer for playback
                    await loadPlayer(path: outFile.path)
                } else {
                    status = "Synthesis failed"
                }
            } catch {
                status = "Error: \(error.localizedDescription)"
            }

            isSynthesizing = false
        }
    }

    private func loadPlayer(path: String) async {
        // Release old player
        playingTask?.cancel()
        player?.release()

        do {
            let newPlayer = try await SonixPlayer.create(source: path)
            await MainActor.run {
                player = newPlayer
                // Observe playback state
                playingTask = newPlayer.observeIsPlaying { self.isPlaying = $0 }
            }
        } catch {
            await MainActor.run {
                status = "Player error: \(error.localizedDescription)"
            }
        }
    }
}

#Preview {
    MidiSection()
        .padding()
}
