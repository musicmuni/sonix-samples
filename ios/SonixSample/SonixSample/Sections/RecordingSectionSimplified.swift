import SwiftUI
import vozos
import AVFoundation

/// Simplified Recording Section using the unified SonixRecorder API.
///
/// Compare this to RecordingSection.swift - this version is much shorter
/// because SonixRecorder handles encoding, buffer management, and level metering.
///
/// Uses type-safe observers (no force casts!) via Swift extensions.
struct RecordingSectionSimplified: View {
    @State private var recorder: SonixRecorder?
    @State private var isRecording = false
    @State private var durationMs: Int64 = 0
    @State private var audioLevel: Float = 0
    @State private var savedFilePath: String?
    @State private var status = "Ready"
    @State private var selectedFormat = "m4a"

    // Observer tasks (for cancellation)
    @State private var recordingTask: Task<Void, Never>?
    @State private var durationTask: Task<Void, Never>?
    @State private var levelTask: Task<Void, Never>?
    @State private var errorTask: Task<Void, Never>?

    // Playback state
    @State private var player: SonixPlayer?
    @State private var isPlaying = false
    @State private var playbackTimeMs: Int64 = 0
    @State private var playbackDurationMs: Int64 = 0

    // Playback observer tasks
    @State private var playingTask: Task<Void, Never>?
    @State private var timeTask: Task<Void, Never>?

    private let formats = ["m4a", "mp3"]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recording (Simplified API)")
                .font(.headline)

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Format selector
            HStack {
                Text("Format:")
                    .font(.caption)
                ForEach(formats, id: \.self) { format in
                    formatButton(format: format)
                }
            }

            Text("Duration: \(formatDuration(durationMs))")
                .font(.body)

            // Audio level indicator
            HStack {
                Text("Level:")
                    .font(.caption)
                ProgressView(value: Double(audioLevel), total: 1.0)
            }

            // Control buttons - THE SIMPLE PART!
            HStack(spacing: 12) {
                Button("Record") {
                    startRecording()
                }
                .disabled(isRecording)
                .buttonStyle(.borderedProminent)

                Button("Stop") {
                    stopRecording()
                }
                .disabled(!isRecording)
                .buttonStyle(.bordered)
            }

            // Show saved file info and playback controls
            if let path = savedFilePath, !isRecording {
                let url = URL(fileURLWithPath: path)
                if let attrs = try? FileManager.default.attributesOfItem(atPath: path),
                   let fileSize = attrs[.size] as? Int64 {
                    Text("Saved: \(url.lastPathComponent) (\(fileSize / 1024) KB)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                // Playback time display
                if playbackDurationMs > 0 {
                    Text("Playback: \(formatDuration(playbackTimeMs)) / \(formatDuration(playbackDurationMs))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                // Playback controls
                HStack(spacing: 12) {
                    Button("Play") {
                        playRecording(path: path)
                    }
                    .disabled(isRecording || isPlaying)
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    Button("Stop Playback") {
                        player?.stop()
                        playbackTimeMs = 0
                        status = "Stopped"
                    }
                    .disabled(player == nil)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }
        }
        .onDisappear {
            cancelAllTasks()
            recorder?.release()
            player?.release()
        }
    }

    @ViewBuilder
    private func formatButton(format: String) -> some View {
        if selectedFormat == format {
            Button(format.uppercased()) {
                selectedFormat = format
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
            .disabled(isRecording)
        } else {
            Button(format.uppercased()) {
                selectedFormat = format
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(isRecording)
        }
    }

    private func startRecording() {
        Task {
            // Request microphone permission
            let granted = await withCheckedContinuation { continuation in
                AVAudioSession.sharedInstance().requestRecordPermission { granted in
                    continuation.resume(returning: granted)
                }
            }
            guard granted else {
                await MainActor.run { status = "Microphone permission denied" }
                return
            }

            await MainActor.run { status = "Starting..." }

            let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let outputPath = documentsPath.appendingPathComponent("recording_\(Int(Date().timeIntervalSince1970)).\(selectedFormat)").path

            // THIS IS ALL IT TAKES TO RECORD!
            let newRecorder = SonixRecorder.create(
                outputPath: outputPath,
                format: selectedFormat,
                quality: "voice"
            )

            await MainActor.run {
                recorder = newRecorder
                savedFilePath = outputPath
            }

            newRecorder.start()

            await MainActor.run {
                isRecording = true
                status = "Recording (\(selectedFormat.uppercased()))"
            }

            // Type-safe state observation (no force casts!)
            await MainActor.run {
                recordingTask = newRecorder.observeIsRecording { self.isRecording = $0 }
                durationTask = newRecorder.observeDuration { self.durationMs = $0 }
                levelTask = newRecorder.observeLevel { self.audioLevel = $0 }
                errorTask = newRecorder.observeError { error in
                    if let err = error {
                        self.status = "Error: \(err.message)"
                    }
                }
            }
        }
    }

    private func stopRecording() {
        recorder?.stop()
        isRecording = false
        audioLevel = 0
        status = "Saved"
    }

    private func playRecording(path: String) {
        Task {
            // Release old player
            playingTask?.cancel()
            timeTask?.cancel()
            player?.release()

            // Create player using unified SonixPlayer API
            let newPlayer = try await SonixPlayer.create(source: path)
            await MainActor.run {
                player = newPlayer
                playbackDurationMs = newPlayer.duration
            }

            // Type-safe state observation (no force casts!)
            await MainActor.run {
                playingTask = newPlayer.observeIsPlaying { self.isPlaying = $0 }
                timeTask = newPlayer.observeCurrentTime { self.playbackTimeMs = $0 }
            }

            newPlayer.play()
            await MainActor.run { status = "Playing recording" }
        }
    }

    private func cancelAllTasks() {
        recordingTask?.cancel()
        durationTask?.cancel()
        levelTask?.cancel()
        errorTask?.cancel()
        playingTask?.cancel()
        timeTask?.cancel()
    }

    private func formatDuration(_ ms: Int64) -> String {
        let seconds = (ms / 1000) % 60
        let minutes = (ms / 1000) / 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

#Preview {
    RecordingSectionSimplified()
        .padding()
}
