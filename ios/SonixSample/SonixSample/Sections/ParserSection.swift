import SwiftUI
import sonix

/// Parser Section demonstrating pitch, notes, and transcription file parsing.
///
/// This section shows how to use the Parser API to parse:
/// - .pitchPP files (pitch contour data)
/// - .notes files (note transcription data)
/// - .trans files (JSON transcription data)
struct ParserSection: View {
    @State private var status = "Ready"
    @State private var pitchSummary: String?
    @State private var notesSummary: String?
    @State private var transSummary: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("File Parsers")
                .font(.headline)

            Text("Status: \(status)")
                .font(.caption)
                .foregroundColor(.secondary)

            // Parse buttons
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    Button("Parse .pitchPP") {
                        parsePitchFile()
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    Button("Parse .notes") {
                        parseNotesFile()
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    Button("Parse .trans") {
                        parseTransFile()
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }

            // Results display
            if let summary = pitchSummary {
                ResultCard(summary: summary)
            }

            if let summary = notesSummary {
                ResultCard(summary: summary)
            }

            if let summary = transSummary {
                ResultCard(summary: summary)
            }
        }
    }

    private func parsePitchFile() {
        Task {
            status = "Parsing pitch file..."
            pitchSummary = nil

            guard let content = loadAssetString(name: "Alankaar 01", ext: "pitchPP") else {
                await MainActor.run { status = "Asset not found" }
                return
            }

            // Parser.parsePitchString() - uses Swift extension for clean API
            if let data = Parser.parsePitchString(content: content) {
                // Convert KotlinFloatArray to Swift [Float]
                let times = (0..<data.times.size).map { data.times.get(index: $0) }
                let pitches = (0..<data.pitchesHz.size).map { data.pitchesHz.get(index: $0) }

                let minTime = times.min() ?? 0
                let maxTime = times.max() ?? 0
                let validPitches = pitches.filter { $0 > 0 }
                let minPitch = validPitches.min() ?? 0
                let maxPitch = validPitches.max() ?? 0

                await MainActor.run {
                    pitchSummary = """
                    Pitch Data (.pitchPP):
                      Points: \(data.count)
                      Time range: \(String(format: "%.2f", minTime)) - \(String(format: "%.2f", maxTime)) sec
                      Pitch range: \(String(format: "%.1f", minPitch)) - \(String(format: "%.1f", maxPitch)) Hz
                      Voiced: \(validPitches.count) points
                    """
                    status = "Pitch file parsed"
                }
            } else {
                await MainActor.run { status = "Failed to parse pitch file" }
            }
        }
    }

    private func parseNotesFile() {
        Task {
            status = "Parsing notes file..."
            notesSummary = nil

            guard let content = loadAssetString(name: "Alankaar 01", ext: "notes") else {
                await MainActor.run { status = "Asset not found" }
                return
            }

            // Parser.parseNotesString() - uses Swift extension for clean API
            if let data = Parser.parseNotesString(content: content) {
                // Convert Kotlin arrays to Swift arrays (explicitly convert NSString to String)
                let labels: [String] = (0..<data.labels.size).map { String(data.labels.get(index: $0) ?? "") }
                let startTimes = (0..<data.startTimes.size).map { data.startTimes.get(index: $0) }
                let endTimes = (0..<data.endTimes.size).map { data.endTimes.get(index: $0) }

                let uniqueLabels = Set(labels)
                let minStart = startTimes.min() ?? 0
                let maxEnd = endTimes.max() ?? 0
                let totalDuration = maxEnd - minStart

                await MainActor.run {
                    notesSummary = """
                    Notes Data (.notes):
                      Notes: \(data.count)
                      Duration: \(String(format: "%.2f", totalDuration)) sec
                      Unique svaras: \(uniqueLabels.count)
                      Labels: \(uniqueLabels.sorted().joined(separator: ", "))
                    """
                    status = "Notes file parsed"
                }
            } else {
                await MainActor.run { status = "Failed to parse notes file" }
            }
        }
    }

    private func parseTransFile() {
        Task {
            status = "Parsing trans file..."
            transSummary = nil

            guard let content = loadAssetString(name: "Alankaar 01", ext: "trans") else {
                await MainActor.run { status = "Asset not found" }
                return
            }

            // Parser.parseTransString() - uses Swift extension for clean API
            if let data = Parser.parseTransString(content: content) {
                // Convert Kotlin List to Swift Array
                let segments = Array(data.segments)
                var totalNotes = 0
                var allLabels = Set<String>()
                var firstLyrics = ""

                for (index, segment) in segments.enumerated() {
                    let trans = Array(segment.trans)
                    totalNotes += trans.count
                    for note in trans {
                        allLabels.insert(note.label)
                    }
                    if index == 0 {
                        firstLyrics = String(segment.lyrics.prefix(30))
                    }
                }

                await MainActor.run {
                    transSummary = """
                    Trans Data (.trans JSON):
                      Segments: \(data.count)
                      Total notes: \(totalNotes)
                      Unique svaras: \(allLabels.count)
                      Labels: \(allLabels.joined(separator: ", "))
                      First lyrics: "\(firstLyrics)..."
                    """
                    status = "Trans file parsed"
                }
            } else {
                await MainActor.run { status = "Failed to parse trans file" }
            }
        }
    }

    /// Load asset file content as string
    private func loadAssetString(name: String, ext: String) -> String? {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else {
            return nil
        }
        return try? String(contentsOf: url, encoding: .utf8)
    }
}

/// Card view for displaying parse results
private struct ResultCard: View {
    let summary: String

    var body: some View {
        Text(summary)
            .font(.caption)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(8)
            .background(Color(.systemGray6))
            .cornerRadius(8)
    }
}

#Preview {
    ParserSection()
        .padding()
}
