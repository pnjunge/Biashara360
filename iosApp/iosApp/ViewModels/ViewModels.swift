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
    let biometrics = BiometricAuthentication()

    func login(email: String, password: String) async {
        isLoading = true; errorMessage = nil
        try? await Task.sleep(nanoseconds: 800_000_000)
        // Demo: any login works
        isLoading = false
        requiresOtp = true
        currentUserId = "demo-user-id"
    }

    func verifyOtp(code: String) async {
        isLoading = true
        try? await Task.sleep(nanoseconds: 600_000_000)
        isLoading = false
        if code == "123456" || code.count == 6 {
            isAuthenticated = true
        } else {
            errorMessage = "Invalid OTP. Use 6 digits."
        }
    }

    func logout() {
        isAuthenticated = false
        requiresOtp = false
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
    @Published var monthRevenue: Double = 145650
    @Published var netProfit: Double = 38200
    @Published var ordersToday: Int = 24
    @Published var pendingPayments: Double = 12300
    @Published var lowStockCount: Int = 3
    @Published var isLoading = false

    struct RecentOrder: Identifiable {
        let id = UUID()
        let number, customer, status: String
        let amount: Double
    }

    @Published var recentOrders: [RecentOrder] = [
        .init(number: "B360-0042", customer: "Amina Hassan", status: "PAID", amount: 4500),
        .init(number: "B360-0041", customer: "Brian Otieno", status: "PENDING", amount: 1500),
        .init(number: "B360-0040", customer: "Grace Njeri", status: "COD", amount: 3200),
        .init(number: "B360-0039", customer: "David Kamau", status: "PAID", amount: 6800),
    ]

    func load() async {
        isLoading = true
        try? await Task.sleep(nanoseconds: 500_000_000)
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

    @Published var products: [Product] = [
        .init(name: "Black Dress Size M",  sku: "SKU-001", category: "Clothing",    buyingPrice: 800,  sellingPrice: 1500, stock: 2,  threshold: 5),
        .init(name: "Ankara Print Fabric", sku: "SKU-002", category: "Fabric",      buyingPrice: 350,  sellingPrice: 700,  stock: 12, threshold: 5),
        .init(name: "Gold Hoop Earrings",  sku: "SKU-003", category: "Accessories", buyingPrice: 150,  sellingPrice: 450,  stock: 3,  threshold: 5),
        .init(name: "White Sneakers 38",   sku: "SKU-004", category: "Shoes",       buyingPrice: 1200, sellingPrice: 2200, stock: 0,  threshold: 3),
        .init(name: "Silk Blouse Pink",    sku: "SKU-005", category: "Clothing",    buyingPrice: 600,  sellingPrice: 1200, stock: 8,  threshold: 5),
        .init(name: "Beaded Necklace",     sku: "SKU-006", category: "Accessories", buyingPrice: 200,  sellingPrice: 600,  stock: 15, threshold: 5),
    ]

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

    @Published var payments: [Payment] = [
        .init(transactionCode: "RGK71HXYZ", payerName: "Amina Hassan", phone: "0712345678", amount: 4500, method: "Mpesa", status: "SUCCESS", date: "Today 2:30PM",  reconciled: true),
        .init(transactionCode: "PLM23NQRS", payerName: "David Kamau",  phone: "0745678901", amount: 6800, method: "Mpesa", status: "SUCCESS", date: "Yesterday",     reconciled: true),
        .init(transactionCode: "QWE45RTYU", payerName: "Sarah Wangui", phone: "0767890123", amount: 1200, method: "Airtel",status: "SUCCESS", date: "Yesterday",     reconciled: false),
        .init(transactionCode: "ZXC89VBNM", payerName: "Tom Mutua",    phone: "0778901234", amount: 2300, method: "Mpesa", status: "SUCCESS", date: "Mon",           reconciled: false),
    ]

    var unreconciled: [Payment] { payments.filter { !$0.reconciled } }
    var totalCollected: Double { payments.filter { $0.reconciled }.reduce(0) { $0 + $1.amount } }
}
