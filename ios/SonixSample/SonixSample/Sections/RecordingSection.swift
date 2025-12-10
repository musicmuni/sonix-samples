import SwiftUI
import sonix
import AVFoundation

/// Recording Section using the unified SonixRecorder Builder API.
///
/// This demonstrates advanced usage with SonixRecorder.Builder() including:
/// - Custom sample rate, channels, and bitrate
/// - Buffer Pool status monitoring (for Calibra integration)
/// - SonixPlayer for playback after recording
///
/// Uses type-safe observers (no force casts!) via Swift extensions.
struct RecordingSection: View {
    @State private var recorder: SonixRecorder?
    @State private var isRecording = false
    @State private var durationMs: Int64 = 0
    @State private var audioLevel: Float = 0
    @State private var savedFilePath: String?
    @State private var status = "Ready"
    @State private var selectedFormat = "m4a"

    // Buffer Pool metrics (for Calibra integration)
    @State private var bufferPoolAvailable: Int32 = 4
    @State private var bufferPoolWasExhausted = false

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
            Text("Recording")
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

            // Buffer pool status (for Calibra integration / zero-alloc monitoring)
            if isRecording {
                Text("Buffer Pool: \(bufferPoolAvailable)/4 available" +
                     (bufferPoolWasExhausted ? " (overflow occurred)" : " (zero-alloc)"))
                    .font(.caption)
                    .foregroundColor(bufferPoolWasExhausted ? .red : .blue)
            }

            // Control buttons
            HStack(spacing: 12) {
                Button("Record") {
                    startRecording()
                }
                .disabled(isRecording)
                .buttonStyle(.borderedProminent)

                Button("Stop & Save") {
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

                    Button("Pause") {
                        player?.pause()
                        status = "Paused"
                    }
                    .disabled(!isPlaying)
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    Button("Stop") {
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

            // Create recorder using Builder for advanced config with callbacks
            let newRecorder = SonixRecorder.Builder()
                .outputPath(path: outputPath)
                .format(name: selectedFormat)
                .sampleRate(rate: 16000)
                .channels(count: 1)
                .bitrate(bps: 128000)
                .onRecordingStarted {
                    print("Recording started!")
                }
                .onRecordingStopped { path in
                    print("Recording saved to: \(path)")
                }
                .onError { error in
                    print("Recording error: \(error)")
                }
                .build()

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
                levelTask = newRecorder.observeLevel { level in
                    self.audioLevel = level
                    // Update buffer pool metrics while recording
                    self.bufferPoolAvailable = newRecorder.bufferPoolAvailable
                    self.bufferPoolWasExhausted = newRecorder.bufferPoolWasExhausted
                }
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
    RecordingSection()
        .padding()
}
