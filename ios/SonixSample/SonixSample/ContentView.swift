import SwiftUI
import sonix

/// API mode toggle: Simple (new Sonix facade) vs Advanced (low-level APIs)
enum ApiMode {
    case simple
    case advanced
}

struct ContentView: View {
    @State private var apiMode: ApiMode = .simple

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // API Mode Toggle
                    apiModeToggle

                    Text(apiMode == .simple
                         ? "Using simplified Sonix API (recommended for most apps)"
                         : "Using low-level com.musicmuni.sonix.api.* (for power users)")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Divider()

                    // Show sections based on API mode - use LazyVStack for deferred loading
                    LazyVStack(spacing: 16) {
                        if apiMode == .simple {
                            SectionCard { RecordingSectionSimplified() }
                            SectionCard { PlaybackSectionSimplified() }
                            SectionCard { MultiTrackSectionSimplified() }
                            SectionCard { MidiSectionSimplified() }
                            SectionCard { MetronomeSectionSimplified() }
                        } else {
                            SectionCard { RecordingSection() }
                            SectionCard { PlaybackSection() }
                            SectionCard { MultiTrackSection() }
                            SectionCard { MidiSection() }
                            SectionCard { MetronomeSection() }
                            SectionCard { DecodingSection() }
                            SectionCard { ParserSection() }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Sonix Sample")
        }
    }

    private var apiModeToggle: some View {
        HStack(spacing: 8) {
            Text("API Mode:")
                .font(.subheadline)

            apiModeButton(mode: .simple, label: "Simple")
            apiModeButton(mode: .advanced, label: "Advanced")
        }
    }

    @ViewBuilder
    private func apiModeButton(mode: ApiMode, label: String) -> some View {
        if apiMode == mode {
            Button(label) {
                apiMode = mode
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
        } else {
            Button(label) {
                apiMode = mode
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }
}

/// Wrapper card for each section to provide visual separation.
struct SectionCard<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)
    }
}

#Preview {
    ContentView()
}
