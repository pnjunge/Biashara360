import SwiftUI

/// Root content is kept separate from the application entry point so previews
/// and UI tests can launch the same authenticated flow without a second @main.
struct ContentView: View {
    @EnvironmentObject private var authVM: AuthViewModel

    var body: some View {
        Group {
            if authVM.isAuthenticated {
                MainTabView()
            } else {
                LoginView()
            }
        }
        .environmentObject(authVM)
    }
}
