import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem { Label("Dashboard", systemImage: "house.fill") }
                .tag(0)

            InventoryView()
                .tabItem { Label("Inventory", systemImage: "shippingbox.fill") }
                .tag(1)

            OrdersView()
                .tabItem { Label("Orders", systemImage: "cart.fill") }
                .tag(2)

            CustomersView()
                .tabItem { Label("Customers", systemImage: "person.2.fill") }
                .tag(3)

            MoreView()
                .tabItem { Label("More", systemImage: "ellipsis.circle.fill") }
                .tag(4)

            CyberSourceView()
                .tabItem { Label("Card Pay", systemImage: "creditcard.and.123") }
                .tag(5)
            TaxView()
                .tabItem { Label("Tax", systemImage: "percent") }
                .tag(6)
            KraView()
                .tabItem { Label("KRA iTax", systemImage: "doc.badge.checkmark") }
                .tag(7)
            SocialView()
                .tabItem { Label("Social", systemImage: "message.badge") }
                .tag(8)
        }
        .accentColor(.b360Green)
    }
}

// ── More Tab ──────────────────────────────────────────────────────────────────
struct MoreView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section("Business") {
                    NavigationLink(destination: ExpensesView()) {
                        Label("Expenses & Profit", systemImage: "receipt.fill")
                    }
                    NavigationLink(destination: PaymentsView()) {
                        Label("Payments / Mpesa", systemImage: "creditcard.fill")
                    }
                    NavigationLink(destination: ReportsView()) {
                        Label("Reports", systemImage: "chart.bar.fill")
                    }
                }

                Section("Account") {
                    NavigationLink(destination: SettingsView()) {
                        Label("Settings", systemImage: "gearshape.fill")
                    }
                    Button(role: .destructive) {
                        authVM.logout()
                    } label: {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .navigationTitle("More")
        }
    }
}
