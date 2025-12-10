import SwiftUI
import sonix

@main
struct SonixSampleApp: App {
    @State private var licenseError: String? = nil

    init() {
        // Initialize Sonix logging first to see debug output in Xcode console
        Sonix.initializeLogging()

        // Initialize Sonix SDK with API key from Config
        // This validates the API key synchronously and throws if invalid
        do {
            try Sonix.initialize(apiKey: Config.sonixAPIKey)
        } catch let error as SonixKilledException {
            // Store error for display
            _licenseError = State(initialValue: error.message ?? "Invalid or revoked API key")
        } catch {
            _licenseError = State(initialValue: "License validation failed: \(error.localizedDescription)")
        }
    }

    var body: some Scene {
        WindowGroup {
            if let error = licenseError {
                LicenseErrorView(message: error)
            } else {
                ContentView()
            }
        }
    }
}

/// View displayed when license validation fails
struct LicenseErrorView: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 60))
                .foregroundColor(.red)

            Text("License Error")
                .font(.title)
                .fontWeight(.semibold)

            Text(message)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)

            Text("Contact support@musicmuni.com")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(32)
    }
}
