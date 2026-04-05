import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Initialize Koin for iOS
        KoinModulesKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem {
                    Label("Nyumbani", systemImage: "house.fill")
                }
            OrdersView()
                .tabItem {
                    Label("Maagizo", systemImage: "cart.fill")
                }
            InventoryView()
                .tabItem {
                    Label("Hifadhi", systemImage: "cube.box.fill")
                }
            CustomersView()
                .tabItem {
                    Label("Wateja", systemImage: "person.2.fill")
                }
            ExpensesView()
                .tabItem {
                    Label("Gharama", systemImage: "banknote.fill")
                }
        }
        .accentColor(Color(red: 0.106, green: 0.545, blue: 0.204)) // B360 Green
    }
}

// MARK: – Dashboard
struct DashboardView: View {
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Revenue card
                    RevenueBanner()
                    QuickActionsRow()
                    StatsGrid()
                }
                .padding()
            }
            .navigationTitle("Biashara360")
            .background(Color(.systemGroupedBackground))
        }
    }
}

struct RevenueBanner: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color(red: 0.106, green: 0.545, blue: 0.204),
                            Color(red: 0.059, green: 0.369, blue: 0.133)
                        ]),
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
            VStack(alignment: .leading, spacing: 8) {
                Text("This Month's Revenue")
                    .foregroundColor(.white.opacity(0.8))
                    .font(.subheadline)
                Text("KES 145,650")
                    .foregroundColor(.white)
                    .font(.system(size: 30, weight: .bold))
                HStack(spacing: 24) {
                    VStack(alignment: .leading) {
                        Text("Net Profit").foregroundColor(.white.opacity(0.7)).font(.caption)
                        Text("KES 38,200").foregroundColor(.white).font(.subheadline.bold())
                    }
                    VStack(alignment: .leading) {
                        Text("Pending").foregroundColor(.white.opacity(0.7)).font(.caption)
                        Text("KES 12,300").foregroundColor(Color.yellow).font(.subheadline.bold())
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
        }
        .frame(height: 140)
    }
}

struct QuickActionsRow: View {
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                QuickActionButton(icon: "plus.circle.fill", label: "New Order", color: .green)
                QuickActionButton(icon: "cube.box.fill", label: "Add Stock", color: .blue)
                QuickActionButton(icon: "doc.text.fill", label: "Add Expense", color: .orange)
                QuickActionButton(icon: "person.badge.plus", label: "New Customer", color: .purple)
            }
        }
    }
}

struct QuickActionButton: View {
    let icon: String, label: String, color: Color
    var body: some View {
        Button(action: {}) {
            HStack {
                Image(systemName: icon).foregroundColor(color)
                Text(label).font(.callout.bold()).foregroundColor(color)
            }
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(color.opacity(0.12))
            .cornerRadius(12)
        }
    }
}

struct StatsGrid: View {
    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            StatCard(title: "Orders Today", value: "24", icon: "cart.fill", color: .green)
            StatCard(title: "Low Stock", value: "3", icon: "exclamationmark.triangle.fill", color: .orange)
            StatCard(title: "Customers", value: "186", icon: "person.2.fill", color: .blue)
            StatCard(title: "Unpaid Orders", value: "7", icon: "clock.fill", color: .red)
        }
    }
}

struct StatCard: View {
    let title: String, value: String, icon: String, color: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title).font(.caption).foregroundColor(.secondary)
                Spacer()
                Image(systemName: icon).foregroundColor(color).font(.caption)
            }
            Text(value).font(.title.bold()).foregroundColor(color)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// MARK: – Placeholder views
struct OrdersView: View {
    var body: some View {
        NavigationView {
            Text("Orders Screen").navigationTitle("Maagizo")
        }
    }
}

struct InventoryView: View {
    var body: some View {
        NavigationView {
            Text("Inventory Screen").navigationTitle("Hifadhi")
        }
    }
}

struct CustomersView: View {
    var body: some View {
        NavigationView {
            Text("Customers Screen").navigationTitle("Wateja")
        }
    }
}

struct ExpensesView: View {
    var body: some View {
        NavigationView {
            Text("Expenses Screen").navigationTitle("Gharama")
        }
    }
}
