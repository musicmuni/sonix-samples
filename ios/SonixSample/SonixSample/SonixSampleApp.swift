import SwiftUI
import sonix

@main
struct SonixSampleApp: App {
    init() {
        // Initialize Sonix SDK with API key from Config
        Sonix.initialize(apiKey: Config.sonixAPIKey)

        // Initialize Sonix logging to see debug output in Xcode console
        Sonix.initializeLogging()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
