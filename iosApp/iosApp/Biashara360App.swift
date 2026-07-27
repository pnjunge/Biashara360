import SwiftUI

@main
struct Biashara360App: App {
    @StateObject private var authVM = AuthViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authVM)
                .tint(.b360Green)
        }
    }
}
