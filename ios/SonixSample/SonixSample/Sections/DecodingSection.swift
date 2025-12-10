import SwiftUI
import sonix

/// Audio Decoding & Encoding Section using SonixDecoder and SonixEncoder.
///
/// Demonstrates:
/// - SonixDecoder.decode() for extracting raw PCM from audio files
/// - SonixEncoder.encode() for encoding raw PCM to compressed formats
/// - SonixEncoder.isFormatAvailable() for checking platform support
/// - Full decode -> re-encode pipeline (format conversion)
struct DecodingSection: View {
    @State private var status = "Ready"
    @State private var isProcessing = false
    @State private var decodedInfo: DecodedAudioInfo?
    @State private var decodedData: AudioRawData?
    @State private var encodedFile: URL?
    @State private var selectedEncodeFormat = "m4a"
    @State private var selectedBitrate: Int32 = 128

    // Check format availability - uses Swift extension for clean API
    private var mp3Available: Bool { SonixEncoder.isFormatAvailable(format: "mp3") }
    private var m4aAvailable: Bool { SonixEncoder.isFormatAvailable(format: "m4a") }

    private let formats = ["m4a", "mp3"]
    private let bitrates: [Int32] = [64, 128, 192, 256]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Audio Decode/Encode (SonixDecoder + SonixEncoder)")
                .font(.headline)

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Format availability info
            Text("Formats: M4A \(m4aAvailable ? "✓" : "✗") | MP3 \(mp3Available ? "✓" : "✗")")
                .font(.caption)
                .foregroundColor(.secondary)

            // Decode button
            Button(isProcessing ? "Processing..." : "Decode sample.m4a") {
                decode()
            }
            .disabled(isProcessing)
            .buttonStyle(.borderedProminent)

            // Show decoded info
            if let info = decodedInfo {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Decoded Audio Info")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    InfoRow(label: "Sample Rate", value: "\(info.sampleRate) Hz")
                    InfoRow(label: "Channels", value: "\(info.channels)")
                    InfoRow(label: "Duration", value: "\(info.durationMs) ms")
                    InfoRow(label: "PCM Size", value: "\(info.dataSize / 1024) KB (\(info.dataSize) bytes)")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }

            // Encoding section - only show if we have decoded data
            if decodedData != nil {
                Divider()
                    .padding(.vertical, 8)

                Text("Re-encode to:")
                    .font(.subheadline)
                    .fontWeight(.medium)

                // Format selection
                HStack(spacing: 8) {
                    ForEach(formats, id: \.self) { format in
                        let available = format == "mp3" ? mp3Available : m4aAvailable
                        formatButton(format: format, available: available)
                    }
                }

                // Bitrate options
                HStack(spacing: 8) {
                    Text("Bitrate:")
                        .font(.caption)
                    ForEach(bitrates, id: \.self) { bitrate in
                        bitrateButton(bitrate: bitrate)
                    }
                }

                // Encode button
                Button("Encode to \(selectedEncodeFormat.uppercased())") {
                    encode()
                }
                .disabled(isProcessing || !SonixEncoder.isFormatAvailable(format: selectedEncodeFormat))
                .buttonStyle(.borderedProminent)

                // Show encoded file info
                if let file = encodedFile {
                    if let attrs = try? FileManager.default.attributesOfItem(atPath: file.path),
                       let fileSize = attrs[.size] as? Int64 {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Encoded File")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            InfoRow(label: "File", value: file.lastPathComponent)
                            InfoRow(label: "Size", value: "\(fileSize / 1024) KB")
                            if let info = decodedInfo {
                                let compression = info.dataSize / Int(fileSize)
                                InfoRow(label: "Compression", value: "\(compression)x")
                            }
                        }
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func formatButton(format: String, available: Bool) -> some View {
        if selectedEncodeFormat == format {
            Button(format.uppercased()) {
                selectedEncodeFormat = format
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
            .disabled(!available || isProcessing)
        } else {
            Button(format.uppercased()) {
                selectedEncodeFormat = format
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(!available || isProcessing)
        }
    }

    @ViewBuilder
    private func bitrateButton(bitrate: Int32) -> some View {
        if selectedBitrate == bitrate {
            Button("\(bitrate)k") {
                selectedBitrate = bitrate
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
            .disabled(isProcessing)
        } else {
            Button("\(bitrate)k") {
                selectedBitrate = bitrate
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(isProcessing)
        }
    }

    private func decode() {
        Task {
            isProcessing = true
            status = "Decoding sample.m4a..."

            guard let audioPath = copyAssetToFile(name: "sample", ext: "m4a") else {
                status = "Asset not found"
                isProcessing = false
                return
            }

            // SonixDecoder.decode() - uses Swift extension for clean API
            if let result = SonixDecoder.decode(path: audioPath) {
                decodedData = result
                decodedInfo = DecodedAudioInfo(
                    sampleRate: result.sampleRate,
                    channels: result.numChannels,
                    durationMs: Int64(result.durationMilliSecs),
                    dataSize: Int(result.audioData.size)
                )
                status = "Decoded successfully"
            } else {
                status = "Decoding failed"
            }

            isProcessing = false
        }
    }

    private func encode() {
        guard let data = decodedData else { return }

        Task {
            isProcessing = true
            status = "Encoding to \(selectedEncodeFormat)..."

            let tempDir = FileManager.default.temporaryDirectory
            let outputPath = tempDir.appendingPathComponent("encoded_output.\(selectedEncodeFormat)").path

            // SonixEncoder.encode() - uses Swift extension for clean API
            let success = SonixEncoder.encode(
                data: data,
                outputPath: outputPath,
                format: selectedEncodeFormat,
                bitrateKbps: selectedBitrate
            )

            if success {
                let file = URL(fileURLWithPath: outputPath)
                encodedFile = file
                if let attrs = try? FileManager.default.attributesOfItem(atPath: outputPath),
                   let fileSize = attrs[.size] as? Int64 {
                    status = "Encoded: \(file.lastPathComponent) (\(fileSize / 1024) KB)"
                } else {
                    status = "Encoded successfully"
                }
            } else {
                status = "Encoding failed"
            }

            isProcessing = false
        }
    }
}

struct DecodedAudioInfo {
    let sampleRate: Int32
    let channels: Int32
    let durationMs: Int64
    let dataSize: Int
}

struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
        }
        .font(.callout)
    }
}

#Preview {
    DecodingSection()
        .padding()
}
