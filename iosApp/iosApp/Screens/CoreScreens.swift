import SwiftUI

// ── Dashboard ─────────────────────────────────────────────────────────────────
struct DashboardView: View {
    @StateObject private var vm = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 20) {
                    // KPI grid
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        KpiCard(title: "Revenue (Month)", value: "KES 145,650", change: "+12% vs last month",  icon: "arrow.up.right",   color: .b360Green)
                        KpiCard(title: "Net Profit",      value: "KES 38,200",  change: "Margin: 26%",         icon: "banknote",         color: .b360Blue)
                        KpiCard(title: "Orders Today",    value: "24",           change: "+3 from yesterday",  icon: "cart.fill",        color: .b360Amber)
                        KpiCard(title: "Unpaid Orders",   value: "KES 12,300",  change: "7 pending",           icon: "clock.badge",      color: .b360Red)
                    }
                    .padding(.horizontal, 16)

                    // Alerts
                    VStack(spacing: 8) {
                        AlertCard(message: "3 products are running low on stock", icon: "exclamationmark.triangle.fill", color: .b360Amber)
                        AlertCard(message: "2 Mpesa payments unreconciled",       icon: "mpesa.icon",                    color: .b360Blue)
                    }
                    .padding(.horizontal, 16)

                    // Recent Orders
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "Recent Orders", actionLabel: "See all")
                            .padding(.horizontal, 16)

                        VStack(spacing: 0) {
                            ForEach(vm.recentOrders) { order in
                                HStack {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(order.number)
                                            .font(.b360Caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(.b360Green)
                                        Text(order.customer)
                                            .font(.b360Body)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 3) {
                                        Text("KES \(Int(order.amount).formatted())")
                                            .font(.b360Caption)
                                            .fontWeight(.semibold)
                                        StatusBadge(text: order.status)
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                                if order.id != vm.recentOrders.last?.id {
                                    B360Divider().padding(.leading, 16)
                                }
                            }
                        }
                        .b360Card()
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .background(Color.b360Surface)
            .navigationTitle("Dashboard")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { } label: {
                        Image(systemName: "bell.badge.fill").foregroundColor(.b360Green)
                    }
                }
            }
        }
    }
}

// ── Inventory ─────────────────────────────────────────────────────────────────
struct InventoryView: View {
    @StateObject private var vm = InventoryViewModel()
    @State private var showAddSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search + filter
                HStack(spacing: 10) {
                    HStack {
                        Image(systemName: "magnifyingglass").foregroundColor(.b360TextSecondary)
                        TextField("Search products...", text: $vm.searchText)
                    }
                    .padding(10)
                    .background(Color.b360Card)
                    .cornerRadius(10)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))

                    Toggle("", isOn: $vm.showLowStockOnly)
                        .labelsHidden()
                        .tint(.b360Amber)
                        .overlay(
                            Text("⚠")
                                .font(.caption)
                                .offset(x: -2)
                                .allowsHitTesting(false)
                        )
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.b360Surface)

                if vm.filtered.isEmpty {
                    EmptyStateView(icon: "shippingbox", title: "No products yet",
                                   message: "Add your first product to start tracking inventory",
                                   buttonTitle: "Add Product") { showAddSheet = true }
                } else {
                    List(vm.filtered) { product in
                        ProductRow(product: product)
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                            .listRowBackground(Color.b360Surface)
                    }
                    .listStyle(.plain)
                    .background(Color.b360Surface)
                }
            }
            .background(Color.b360Surface)
            .navigationTitle("Inventory")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showAddSheet = true } label: {
                        Image(systemName: "plus.circle.fill").foregroundColor(.b360Green)
                    }
                }
            }
            .sheet(isPresented: $showAddSheet) {
                AddProductSheet()
            }
        }
    }
}

struct ProductRow: View {
    let product: InventoryViewModel.Product

    var stockColor: Color {
        switch product.stockStatus {
        case "OUT": return .b360Red
        case "LOW": return .b360Amber
        default: return .b360Green
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // Stock indicator
            RoundedRectangle(cornerRadius: 4)
                .fill(stockColor)
                .frame(width: 4, height: 44)

            VStack(alignment: .leading, spacing: 3) {
                Text(product.name).font(.b360Body).fontWeight(.medium)
                Text("\(product.sku) · \(product.category)")
                    .font(.b360Caption).foregroundColor(.b360TextSecondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 3) {
                Text("KES \(Int(product.sellingPrice).formatted())")
                    .font(.b360Caption).fontWeight(.semibold).foregroundColor(.b360Green)
                HStack(spacing: 4) {
                    Text("Qty: \(product.stock)")
                        .font(.b360Small).foregroundColor(stockColor).fontWeight(.bold)
                    StatusBadge(text: product.stockStatus)
                }
            }
        }
        .padding(.vertical, 4)
        .b360Card()
        .padding(.vertical, 2)
    }
}

struct AddProductSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var name = ""; @State private var sku = ""
    @State private var buyPrice = ""; @State private var sellPrice = ""
    @State private var stock = ""; @State private var category = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Product Details") {
                    TextField("Product name", text: $name)
                    TextField("SKU / Product ID", text: $sku)
                    TextField("Category", text: $category)
                }
                Section("Pricing") {
                    TextField("Buying price (KES)", text: $buyPrice).keyboardType(.decimalPad)
                    TextField("Selling price (KES)", text: $sellPrice).keyboardType(.decimalPad)
                }
                Section("Stock") {
                    TextField("Initial stock quantity", text: $stock).keyboardType(.numberPad)
                }
            }
            .navigationTitle("Add Product")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { dismiss() }
                        .fontWeight(.semibold)
                        .foregroundColor(.b360Green)
                }
            }
        }
    }
}

// ── Orders ────────────────────────────────────────────────────────────────────
struct OrdersView: View {
    @StateObject private var vm = OrdersViewModel()
    @State private var showCreateSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Filter pills
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(vm.statusFilters, id: \.self) { filter in
                            FilterPill(title: filter, isSelected: vm.filterStatus == filter) {
                                vm.filterStatus = filter
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }
                .background(Color.b360Surface)

                List(vm.filtered) { order in
                    NavigationLink(destination: OrderDetailView(order: order)) {
                        OrderRow(order: order)
                    }
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    .listRowBackground(Color.b360Surface)
                }
                .listStyle(.plain)
                .background(Color.b360Surface)
            }
            .background(Color.b360Surface)
            .navigationTitle("Orders")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showCreateSheet = true } label: {
                        Image(systemName: "plus.circle.fill").foregroundColor(.b360Green)
                    }
                }
            }
        }
    }
}

struct FilterPill: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.b360Caption).fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 14).padding(.vertical, 7)
                .background(isSelected ? Color.b360Green : Color.b360Card)
                .foregroundColor(isSelected ? .white : .b360TextPrimary)
                .cornerRadius(20)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(isSelected ? Color.clear : Color.b360Border))
        }
    }
}

struct OrderRow: View {
    let order: OrdersViewModel.Order

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(order.number).font(.b360Caption).fontWeight(.bold).foregroundColor(.b360Green)
                        Text("· \(order.items) item\(order.items > 1 ? "s" : "")")
                            .font(.b360Small).foregroundColor(.b360TextSecondary)
                    }
                    Text(order.customerName).font(.b360Body).fontWeight(.medium)
                    Text(order.customerPhone).font(.b360Small).foregroundColor(.b360TextSecondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("KES \(Int(order.amount).formatted())")
                        .font(.b360Body).fontWeight(.bold)
                    StatusBadge(text: order.paymentStatus)
                    Text(order.date).font(.b360Small).foregroundColor(.b360TextSecondary)
                }
            }
            .padding(14)
        }
        .b360Card()
        .padding(.vertical, 2)
    }
}

struct OrderDetailView: View {
    let order: OrdersViewModel.Order
    @State private var showMpesaSheet = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Header card
                VStack(spacing: 12) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(order.number).font(.b360Title).foregroundColor(.b360Green)
                            Text(order.date).font(.b360Caption).foregroundColor(.b360TextSecondary)
                        }
                        Spacer()
                        StatusBadge(text: order.paymentStatus)
                    }
                    B360Divider()
                    LabelValue(label: "Customer",  value: order.customerName)
                    LabelValue(label: "Phone",     value: order.customerPhone)
                    LabelValue(label: "Delivery",  value: order.deliveryLocation)
                    LabelValue(label: "Status",    value: order.deliveryStatus)
                    B360Divider()
                    HStack {
                        Text("Total").fontWeight(.bold)
                        Spacer()
                        Text("KES \(Int(order.amount).formatted())").fontWeight(.bold).foregroundColor(.b360Green)
                    }
                }
                .padding(16)
                .b360Card()
                .padding(.horizontal, 16)

                // Action buttons
                if order.paymentStatus == "PENDING" {
                    B360Button(title: "Request Mpesa Payment", icon: "creditcard.fill") {
                        showMpesaSheet = true
                    }
                    .padding(.horizontal, 16)
                }
            }
            .padding(.top, 12)
        }
        .background(Color.b360Surface)
        .navigationTitle(order.number)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showMpesaSheet) {
            MpesaPaymentSheet(orderNumber: order.number, amount: order.amount, phone: order.customerPhone)
        }
    }
}

struct LabelValue: View {
    let label, value: String
    var body: some View {
        HStack {
            Text(label).font(.b360Caption).foregroundColor(.b360TextSecondary)
            Spacer()
            Text(value).font(.b360Caption).fontWeight(.medium)
        }
    }
}

struct MpesaPaymentSheet: View {
    @Environment(\.dismiss) var dismiss
    let orderNumber, phone: String
    let amount: Double
    @State private var phoneInput = ""
    @State private var isSending = false
    @State private var sent = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Image(systemName: "phone.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.b360Green)
                    .padding(.top, 32)

                VStack(spacing: 4) {
                    Text("Request Mpesa Payment")
                        .font(.b360Title)
                    Text("Order \(orderNumber)")
                        .font(.b360Body).foregroundColor(.b360TextSecondary)
                }

                VStack(spacing: 4) {
                    Text("KES \(Int(amount).formatted())")
                        .font(.system(size: 40, weight: .black, design: .rounded))
                        .foregroundColor(.b360Green)
                }

                B360TextField(label: "Customer Phone", placeholder: "0712 345 678",
                              text: $phoneInput, icon: "phone", keyboardType: .phonePad)
                .padding(.horizontal, 24)
                .onAppear { phoneInput = phone }

                if sent {
                    AlertCard(message: "STK Push sent! Customer will see a prompt on their phone.",
                              icon: "checkmark.circle.fill", color: .b360Green)
                    .padding(.horizontal, 24)
                }

                B360Button(title: isSending ? "Sending..." : "Send STK Push",
                           icon: "paperplane.fill", isLoading: isSending) {
                    isSending = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        isSending = false; sent = true
                    }
                }
                .padding(.horizontal, 24)
                .disabled(sent)

                Spacer()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// ── Customers ─────────────────────────────────────────────────────────────────
struct CustomersView: View {
    @StateObject private var vm = CustomersViewModel()
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass").foregroundColor(.b360TextSecondary)
                    TextField("Search customers...", text: $vm.searchText)
                }
                .padding(10)
                .background(Color.b360Card)
                .cornerRadius(10)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.b360Surface)

                List(vm.filtered) { customer in
                    NavigationLink(destination: CustomerDetailView(customer: customer)) {
                        CustomerRow(customer: customer)
                    }
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    .listRowBackground(Color.b360Surface)
                }
                .listStyle(.plain)
                .background(Color.b360Surface)
            }
            .background(Color.b360Surface)
            .navigationTitle("Customers")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showAdd = true } label: {
                        Image(systemName: "person.badge.plus").foregroundColor(.b360Green)
                    }
                }
            }
        }
    }
}

struct CustomerRow: View {
    let customer: CustomersViewModel.Customer

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(name: customer.name)
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(customer.name).font(.b360Body).fontWeight(.medium)
                    if customer.isRepeat {
                        Image(systemName: "star.fill")
                            .font(.system(size: 10)).foregroundColor(.b360Amber)
                    }
                }
                Text(customer.phone).font(.b360Small).foregroundColor(.b360TextSecondary)
                Text(customer.location).font(.b360Small).foregroundColor(.b360TextSecondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                Text("KES \(Int(customer.totalSpent).formatted())")
                    .font(.b360Caption).fontWeight(.semibold).foregroundColor(.b360Green)
                Text("\(customer.totalOrders) orders")
                    .font(.b360Small).foregroundColor(.b360TextSecondary)
                HStack(spacing: 2) {
                    Image(systemName: "star.fill").font(.system(size: 9)).foregroundColor(.b360Amber)
                    Text("\(customer.loyaltyPoints) pts").font(.b360Small).foregroundColor(.b360Amber)
                }
            }
        }
        .padding(12)
        .b360Card()
        .padding(.vertical, 2)
    }
}

struct CustomerDetailView: View {
    let customer: CustomersViewModel.Customer

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Profile card
                VStack(spacing: 14) {
                    AvatarView(name: customer.name, size: 64)
                    Text(customer.name).font(.b360Title)
                    if customer.isRepeat {
                        Label("Repeat Customer", systemImage: "star.fill")
                            .font(.b360Caption).foregroundColor(.b360Amber)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(20)
                .b360Card()
                .padding(.horizontal, 16)

                // Stats
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    StatTile(title: "Orders",   value: "\(customer.totalOrders)",                      color: .b360Blue)
                    StatTile(title: "Spent",    value: "KES\n\(Int(customer.totalSpent/1000))K",        color: .b360Green)
                    StatTile(title: "Points",   value: "\(customer.loyaltyPoints)★",                   color: .b360Amber)
                }
                .padding(.horizontal, 16)

                // Contact info
                VStack(spacing: 0) {
                    ContactRow(icon: "phone.fill",  value: customer.phone)
                    if let email = customer.email {
                        B360Divider()
                        ContactRow(icon: "envelope.fill", value: email)
                    }
                    B360Divider()
                    ContactRow(icon: "location.fill", value: customer.location)
                }
                .b360Card()
                .padding(.horizontal, 16)

                // WhatsApp action
                B360Button(title: "Message on WhatsApp", icon: "message.fill") {}
                    .padding(.horizontal, 16)
            }
            .padding(.top, 12)
        }
        .background(Color.b360Surface)
        .navigationTitle(customer.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct StatTile: View {
    let title, value: String
    let color: Color
    var body: some View {
        VStack(spacing: 6) {
            Text(value).font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundColor(color).multilineTextAlignment(.center)
            Text(title).font(.b360Small).foregroundColor(.b360TextSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .b360Card()
    }
}

struct ContactRow: View {
    let icon, value: String
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon).foregroundColor(.b360Green).frame(width: 20)
            Text(value).font(.b360Body)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}
