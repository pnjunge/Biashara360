import Foundation
import Combine

// ── Auth ──────────────────────────────────────────────────────────────────────
@MainActor
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var requiresOtp = false
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var currentUserId: String = ""
    @Published var otpChannel: String = "SMS"
    let biometrics = BiometricAuthentication()

    func login(email: String, password: String) async {
        guard !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, password.count >= 6 else {
            errorMessage = "Enter a valid email and password."
            return
        }
        isLoading = true; errorMessage = nil
        defer { isLoading = false }
        do {
            let result = try await APIClient.shared.login(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
            currentUserId = result.userId
            otpChannel = result.otpChannels.first ?? "SMS"
            requiresOtp = result.requiresOtp
            if !result.requiresOtp, let access = result.accessToken, let refresh = result.refreshToken {
                try SessionStore.save(accessToken: access, refreshToken: refresh)
                isAuthenticated = true
            }
        } catch { errorMessage = error.localizedDescription }
    }

    func verifyOtp(code: String) async {
        guard code.count == 6 else { errorMessage = "Enter the 6-digit code."; return }
        isLoading = true; errorMessage = nil
        defer { isLoading = false }
        do {
            let result = try await APIClient.shared.verifyOTP(userId: currentUserId, otp: code, channel: otpChannel)
            currentUserId = result.user.id
            isAuthenticated = true
            requiresOtp = false
        } catch { errorMessage = error.localizedDescription }
    }

    func logout() {
        isAuthenticated = false
        requiresOtp = false
        SessionStore.clear()
    }

    func loginWithBiometrics() async {
        guard biometrics.isEnabled else {
            errorMessage = "Enable biometric login in Settings after signing in."
            return
        }
        do {
            currentUserId = try await biometrics.authenticateAndRestoreAccount(
                reason: "Sign in to Biashara360."
            )
            guard SessionStore.hasSession else { throw BiometricAuthenticationError.missingAccount }
            isAuthenticated = true
            errorMessage = nil
        } catch {
            errorMessage = "Biometric authentication was not completed."
        }
    }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
@MainActor
class DashboardViewModel: ObservableObject {
    @Published var monthRevenue: Double = 0
    @Published var netProfit: Double = 0
    @Published var ordersToday: Int = 0
    @Published var pendingPayments: Double = 0
    @Published var lowStockCount: Int = 0
    @Published var isLoading = false

    struct RecentOrder: Identifiable {
        let id = UUID()
        let number, customer, status: String
        let amount: Double
    }

    @Published var recentOrders: [RecentOrder] = []

    func load() async {
        isLoading = true
        isLoading = false
    }
}

// ── Inventory ─────────────────────────────────────────────────────────────────
@MainActor
class InventoryViewModel: ObservableObject {
    struct Product: Identifiable {
        let id = UUID()
        let name, sku, category: String
        let buyingPrice, sellingPrice: Double
        var stock: Int
        let threshold: Int
        var stockStatus: String { stock == 0 ? "OUT" : stock <= threshold ? "LOW" : "OK" }
        var profit: Double { sellingPrice - buyingPrice }
        var margin: Double { sellingPrice > 0 ? profit / sellingPrice * 100 : 0 }
    }

    @Published var products: [Product] = []

    @Published var searchText = ""
    @Published var showLowStockOnly = false

    var filtered: [Product] {
        products.filter {
            (searchText.isEmpty || $0.name.localizedCaseInsensitiveContains(searchText) || $0.sku.localizedCaseInsensitiveContains(searchText))
            && (!showLowStockOnly || $0.stockStatus != "OK")
        }
    }

    var lowStockProducts: [Product] { products.filter { $0.stockStatus != "OK" } }

    func addProduct(
        name: String,
        sku: String,
        category: String,
        buyingPrice: Double,
        sellingPrice: Double,
        stock: Int,
        threshold: Int
    ) {
        products.insert(
            Product(
                name: name,
                sku: sku,
                category: category,
                buyingPrice: buyingPrice,
                sellingPrice: sellingPrice,
                stock: stock,
                threshold: threshold
            ),
            at: 0
        )
    }
}

// ── Orders ────────────────────────────────────────────────────────────────────
@MainActor
class OrdersViewModel: ObservableObject {
    struct Order: Identifiable {
        let id = UUID()
        let number, customerName, customerPhone, deliveryLocation: String
        var paymentStatus, deliveryStatus: String
        let amount: Double
        let date: String
        let items: Int
    }

    @Published var orders: [Order] = []

    @Published var filterStatus = "All"
    let statusFilters = ["All", "PAID", "PENDING", "COD"]

    var filtered: [Order] {
        filterStatus == "All" ? orders : orders.filter { $0.paymentStatus == filterStatus }
    }

    func addOrder(customerName: String, phone: String, amount: Double) {
        orders.insert(
            Order(
                number: "B360-IOS-\(String(UUID().uuidString.prefix(8)).uppercased())",
                customerName: customerName,
                customerPhone: phone,
                deliveryLocation: "Pickup",
                paymentStatus: "PENDING",
                deliveryStatus: "NEW",
                amount: amount,
                date: "Today",
                items: 1
            ),
            at: 0
        )
    }
}

// ── Customers ─────────────────────────────────────────────────────────────────
@MainActor
class CustomersViewModel: ObservableObject {
    struct Customer: Identifiable {
        let id = UUID()
        let name, phone: String
        let email: String?
        let location: String
        let totalOrders: Int
        let totalSpent: Double
        let loyaltyPoints: Int
        var isRepeat: Bool { totalOrders > 1 }
    }

    @Published var customers: [Customer] = []

    @Published var searchText = ""

    var filtered: [Customer] {
        searchText.isEmpty ? customers : customers.filter {
            $0.name.localizedCaseInsensitiveContains(searchText) || $0.phone.contains(searchText)
        }
    }

    func addCustomer(name: String, phone: String, email: String?, location: String) {
        customers.insert(
            Customer(
                name: name,
                phone: phone,
                email: email?.isEmpty == true ? nil : email,
                location: location,
                totalOrders: 0,
                totalSpent: 0,
                loyaltyPoints: 0
            ),
            at: 0
        )
    }
}

// ── Expenses ──────────────────────────────────────────────────────────────────
@MainActor
class ExpensesViewModel: ObservableObject {
    struct Expense: Identifiable {
        let id = UUID()
        let description, category, date: String
        let amount: Double
    }

    @Published var expenses: [Expense] = []

    var totalThisMonth: Double { expenses.reduce(0) { $0 + $1.amount } }

    func categoryColor(_ cat: String) -> String {
        switch cat {
        case "ADVERTISING": return "blue"
        case "RENT": return "red"
        case "STOCK_PURCHASE": return "green"
        default: return "orange"
        }
    }
}

// ── Payments ──────────────────────────────────────────────────────────────────
@MainActor
class PaymentsViewModel: ObservableObject {
    struct Payment: Identifiable {
        let id = UUID()
        let transactionCode, payerName, phone: String
        let amount: Double
        let method, status, date: String
        let reconciled: Bool
    }

    @Published var payments: [Payment] = []

    var unreconciled: [Payment] { payments.filter { !$0.reconciled } }
    var totalCollected: Double { payments.filter { $0.reconciled }.reduce(0) { $0 + $1.amount } }
}
