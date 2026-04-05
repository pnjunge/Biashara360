import SwiftUI

// ── CyberSource View Model ────────────────────────────────────────────────────
@MainActor
class CyberSourceViewModel: ObservableObject {
    struct CsTransaction: Identifiable {
        let id = UUID()
        let csId, orderId, type_, cardLast4, cardType, status, approvalCode, date: String
        let amount: Double
    }

    struct SavedCard: Identifiable {
        let id: String
        let last4, type_, expiry, holder: String
        let isDefault: Bool
    }

    @Published var transactions: [CsTransaction] = [
        .init(csId: "7285900622826740503954", orderId: "B360-0042", type_: "CAPTURE",       cardLast4: "4242", cardType: "VISA",       status: "CAPTURED",   approvalCode: "HH8765", date: "Today 14:32",  amount: 4500),
        .init(csId: "7285900622826740503955", orderId: "B360-0041", type_: "AUTHORIZATION", cardLast4: "5555", cardType: "MASTERCARD", status: "AUTHORIZED", approvalCode: "AB1234", date: "Today 11:05",  amount: 1500),
        .init(csId: "7285900622826740503956", orderId: "B360-0039", type_: "REFUND",        cardLast4: "4242", cardType: "VISA",       status: "REFUNDED",   approvalCode: "",       date: "Yesterday",    amount: 6800),
        .init(csId: "7285900622826740503957", orderId: "B360-0037", type_: "AUTHORIZATION", cardLast4: "4111", cardType: "VISA",       status: "DECLINED",   approvalCode: "",       date: "Mon",          amount: 2200),
    ]

    @Published var savedCards: [SavedCard] = [
        .init(id: "1", last4: "4242", type_: "VISA",       expiry: "12/27", holder: "Amina Hassan",  isDefault: true),
        .init(id: "2", last4: "5555", type_: "MASTERCARD", expiry: "06/26", holder: "Brian Otieno",  isDefault: false),
    ]

    // Payment result
    @Published var lastResult: PaymentResult? = nil
    @Published var isProcessing = false

    struct PaymentResult {
        let success: Bool
        let status, approvalCode, cardLast4, cardType: String
        let reconciliationId: String
    }

    func charge(orderId: String, amount: Double, cardNum: String, expiry: String, cvv: String, name: String, saveCard: Bool) async {
        isProcessing = true
        try? await Task.sleep(nanoseconds: 1_800_000_000)
        let raw = cardNum.replacingOccurrences(of: " ", with: "")
        if raw == "4111111111111111" {
            lastResult = PaymentResult(success: false, status: "DECLINED", approvalCode: "", cardLast4: raw.suffix(4).description, cardType: "VISA", reconciliationId: "")
        } else {
            let cardType = raw.hasPrefix("4") ? "VISA" : raw.hasPrefix("5") ? "MASTERCARD" : "AMEX"
            lastResult = PaymentResult(success: true, status: "CAPTURED", approvalCode: "HH\(Int.random(in: 1000...9999))", cardLast4: raw.suffix(4).description, cardType: cardType, reconciliationId: "\(Date().timeIntervalSince1970)")
        }
        isProcessing = false
    }

    func statusColor(_ s: String) -> Color {
        switch s {
        case "CAPTURED":   return .b360Green
        case "AUTHORIZED": return .b360Blue
        case "REFUNDED":   return .b360Amber
        case "DECLINED":   return .b360Red
        default:           return .b360TextSecondary
        }
    }
}

// ── Main CyberSource View ─────────────────────────────────────────────────────
struct CyberSourceView: View {
    @StateObject private var vm = CyberSourceViewModel()
    @State private var selectedTab = 0

    var captured: Double { vm.transactions.filter { $0.status == "CAPTURED" }.reduce(0) { $0 + $1.amount } }
    var authorized: Double { vm.transactions.filter { $0.status == "AUTHORIZED" }.reduce(0) { $0 + $1.amount } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // KPI cards
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        KpiCard(title: "Captured",    value: "KES \(Int(captured).formatted())",   change: "Settled",         icon: "checkmark.circle.fill", color: .b360Green)
                        KpiCard(title: "Authorized",  value: "KES \(Int(authorized).formatted())", change: "Pending capture",  icon: "clock.fill",            color: .b360Blue)
                        KpiCard(title: "Transactions",value: "\(vm.transactions.count)",            change: "This month",       icon: "creditcard.fill",       color: .b360Amber)
                        KpiCard(title: "Saved Cards", value: "\(vm.savedCards.count)",              change: "Tokenized",        icon: "lock.fill",             color: .b360Blue)
                    }
                    .padding(.horizontal, 16)

                    // Tab selector
                    Picker("", selection: $selectedTab) {
                        Text("Charge Card").tag(0)
                        Text("History").tag(1)
                        Text("Saved Cards").tag(2)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)

                    switch selectedTab {
                    case 0: ChargeCardView(vm: vm)
                    case 1: TransactionHistoryView(vm: vm)
                    default: SavedCardsView(vm: vm)
                    }
                }
                .padding(.vertical, 8)
            }
            .background(Color.b360Surface)
            .navigationTitle("Card Payments")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

// ── Charge Card View ──────────────────────────────────────────────────────────
struct ChargeCardView: View {
    @ObservedObject var vm: CyberSourceViewModel
    @State private var orderId = "B360-0042"
    @State private var amount = "4500"
    @State private var showForm = false
    @State private var useType = 0  // 0=saved, 1=new

    var body: some View {
        VStack(spacing: 16) {
            // Order info
            VStack(alignment: .leading, spacing: 14) {
                Text("Order Details")
                    .font(.b360Headline)
                B360TextField(label: "Order ID", placeholder: "B360-XXXX", text: $orderId, icon: "number")
                B360TextField(label: "Amount (KES)", placeholder: "4500", text: $amount, icon: "banknote", keyboardType: .decimalPad)

                Picker("Payment method", selection: $useType) {
                    Text("🔐 Saved Card").tag(0)
                    Text("💳 New Card").tag(1)
                }
                .pickerStyle(.segmented)
            }
            .padding(16)
            .b360Card()
            .padding(.horizontal, 16)

            // Saved cards
            if useType == 0 {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Saved Cards").font(.b360Headline).padding(.horizontal, 16)
                    ForEach(vm.savedCards) { card in
                        SavedCardRow(card: card)
                            .padding(.horizontal, 16)
                    }
                }
                B360Button(title: "Charge Saved Card", icon: "creditcard.fill") {
                    showForm = true
                }
                .padding(.horizontal, 16)
            } else {
                B360Button(title: "Open Secure Checkout", icon: "lock.shield.fill") {
                    showForm = true
                }
                .padding(.horizontal, 16)
            }

            // Security badge
            HStack(spacing: 6) {
                Image(systemName: "lock.shield.fill").foregroundColor(.b360Green).font(.system(size: 12))
                Text("PCI DSS Level 1 · CyberSource Unified Checkout")
                    .font(.b360Small).foregroundColor(.b360TextSecondary)
            }
        }
        .sheet(isPresented: $showForm) {
            CardPaymentSheet(vm: vm, orderId: orderId, amount: Double(amount) ?? 0)
        }
    }
}

struct SavedCardRow: View {
    let card: CyberSourceViewModel.SavedCard

    var typeColor: Color { card.type_ == "VISA" ? .b360Blue : card.type_ == "MASTERCARD" ? .b360Red : .b360Amber }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "creditcard.fill")
                .foregroundColor(typeColor)
                .frame(width: 36, height: 36)
                .background(typeColor.opacity(0.1))
                .cornerRadius(8)
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(card.type_).font(.b360Small).fontWeight(.black).foregroundColor(typeColor)
                    Text("•••• \(card.last4)").font(.b360Body).fontWeight(.semibold)
                    if card.isDefault {
                        Text("DEFAULT").font(.b360Small).foregroundColor(.b360Green).fontWeight(.bold)
                    }
                }
                Text("\(card.holder) · Exp \(card.expiry)").font(.b360Small).foregroundColor(.b360TextSecondary)
            }
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(card.isDefault ? .b360Green : .clear)
        }
        .padding(12)
        .b360Card()
    }
}

// ── Card Payment Sheet ────────────────────────────────────────────────────────
struct CardPaymentSheet: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var vm: CyberSourceViewModel
    let orderId: String
    let amount: Double

    @State private var cardNum = ""
    @State private var expiry = ""
    @State private var cvv = ""
    @State private var name = ""
    @State private var saveCard = false
    @State private var showResult = false

    // Format helpers
    func fmtCard(_ v: String) -> String {
        let d = v.filter { $0.isNumber }.prefix(16)
        var result = ""
        for (i, c) in d.enumerated() { if i % 4 == 0 && i > 0 { result += " " }; result.append(c) }
        return result
    }
    func fmtExpiry(_ v: String) -> String {
        let d = v.filter { $0.isNumber }.prefix(4)
        return d.count > 2 ? "\(d.prefix(2))/\(d.dropFirst(2))" : String(d)
    }

    var cardType: String {
        let raw = cardNum.filter { $0.isNumber }
        if raw.hasPrefix("4") { return "VISA" }
        if raw.hasPrefix("5") || raw.hasPrefix("2") { return "MASTERCARD" }
        if raw.hasPrefix("3") { return "AMEX" }
        return ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Security header
                    VStack(spacing: 4) {
                        Image(systemName: "lock.shield.fill")
                            .font(.system(size: 36)).foregroundColor(.b360Green)
                        Text("Secure Card Payment").font(.b360Headline)
                        Text("Order \(orderId) · KES \(Int(amount).formatted())")
                            .font(.b360Caption).foregroundColor(.b360TextSecondary)
                    }
                    .padding(.top, 8)

                    // Card form
                    VStack(spacing: 14) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Card Number").font(.b360Caption).foregroundColor(.b360TextSecondary)
                            HStack {
                                TextField("1234 5678 9012 3456", text: Binding(
                                    get: { cardNum },
                                    set: { cardNum = fmtCard($0) }
                                ))
                                .keyboardType(.numberPad)
                                .font(.system(size: 16, weight: .medium, design: .monospaced))
                                if !cardType.isEmpty {
                                    Text(cardType).font(.system(size: 9, weight: .black))
                                        .padding(.horizontal, 5).padding(.vertical, 2)
                                        .background(Color.b360Blue.opacity(0.1))
                                        .foregroundColor(.b360Blue).cornerRadius(3)
                                }
                            }
                            .padding(12)
                            .background(Color.b360Surface)
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
                        }

                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Expiry (MM/YY)").font(.b360Caption).foregroundColor(.b360TextSecondary)
                                TextField("12/27", text: Binding(get: { expiry }, set: { expiry = fmtExpiry($0) }))
                                    .keyboardType(.numberPad).font(.system(size: 15, design: .monospaced))
                                    .padding(12).background(Color.b360Surface).cornerRadius(10)
                                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                Text("CVV").font(.b360Caption).foregroundColor(.b360TextSecondary)
                                SecureField("123", text: $cvv)
                                    .keyboardType(.numberPad).font(.system(size: 15, design: .monospaced))
                                    .padding(12).background(Color.b360Surface).cornerRadius(10)
                                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
                            }
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            Text("Cardholder Name").font(.b360Caption).foregroundColor(.b360TextSecondary)
                            TextField("AMINA HASSAN", text: $name)
                                .textCase(.uppercase)
                                .padding(12).background(Color.b360Surface).cornerRadius(10)
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
                        }

                        Toggle("Save card for future payments", isOn: $saveCard).tint(.b360Green)

                        if vm.isProcessing {
                            HStack(spacing: 10) {
                                ProgressView().tint(.b360Green)
                                Text("Processing with CyberSource...").font(.b360Caption).foregroundColor(.b360TextSecondary)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(14)
                            .background(Color.b360GreenBg)
                            .cornerRadius(10)
                        }
                    }
                    .padding(20)
                    .b360Card()
                    .padding(.horizontal, 16)

                    // Test card hint
                    Text("🧪 Sandbox: any card works except 4111 1111 1111 1111 (decline)")
                        .font(.system(size: 11)).foregroundColor(.b360TextSecondary)
                        .multilineTextAlignment(.center).padding(.horizontal, 16)

                    B360Button(title: vm.isProcessing ? "Processing..." : "Pay KES \(Int(amount).formatted())", icon: "lock.shield.fill", isLoading: vm.isProcessing) {
                        Task {
                            await vm.charge(orderId: orderId, amount: amount, cardNum: cardNum, expiry: expiry, cvv: cvv, name: name, saveCard: saveCard)
                            showResult = true
                        }
                    }
                    .disabled(cardNum.isEmpty || expiry.isEmpty || cvv.isEmpty || name.isEmpty)
                    .padding(.horizontal, 16)

                    HStack(spacing: 4) {
                        Image(systemName: "lock.fill").font(.system(size: 10))
                        Text("PCI DSS Level 1 · 256-bit TLS · CyberSource").font(.system(size: 10))
                    }
                    .foregroundColor(.b360TextSecondary)
                    .padding(.bottom, 16)
                }
            }
            .background(Color.b360Surface.ignoresSafeArea())
            .navigationTitle("Secure Payment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .sheet(isPresented: $showResult) {
            if let result = vm.lastResult {
                PaymentResultView(result: result) { dismiss() }
            }
        }
    }
}

// ── Payment Result View ───────────────────────────────────────────────────────
struct PaymentResultView: View {
    let result: CyberSourceViewModel.PaymentResult
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: result.success ? "checkmark.circle.fill" : "xmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(result.success ? .b360Green : .b360Red)

            VStack(spacing: 6) {
                Text(result.success ? "Payment Successful!" : "Payment Declined")
                    .font(.b360Title)
                    .foregroundColor(result.success ? .b360Green : .b360Red)
                Text(result.success ? "Card has been charged" : "Please try a different card")
                    .font(.b360Body).foregroundColor(.b360TextSecondary)
            }

            if result.success {
                VStack(spacing: 0) {
                    ResultRow(label: "Status",           value: result.status)
                    B360Divider()
                    ResultRow(label: "Approval Code",    value: result.approvalCode)
                    B360Divider()
                    ResultRow(label: "Card",             value: "\(result.cardType) ••\(result.cardLast4)")
                    B360Divider()
                    ResultRow(label: "Reconciliation",   value: String(result.reconciliationId.prefix(16)) + "…")
                }
                .b360Card()
                .padding(.horizontal, 24)
            }

            B360Button(title: "Done", icon: "checkmark") { onDone() }
                .padding(.horizontal, 24)
            Spacer()
        }
        .background(Color.b360Surface.ignoresSafeArea())
    }
}

struct ResultRow: View {
    let label, value: String
    var body: some View {
        HStack {
            Text(label).font(.b360Caption).foregroundColor(.b360TextSecondary)
            Spacer()
            Text(value).font(.b360Caption).fontWeight(.semibold)
                .font(.system(.caption, design: .monospaced))
        }
        .padding(.horizontal, 16).padding(.vertical, 11)
    }
}

// ── Transaction History View ──────────────────────────────────────────────────
struct TransactionHistoryView: View {
    @ObservedObject var vm: CyberSourceViewModel

    var body: some View {
        VStack(spacing: 0) {
            ForEach(vm.transactions) { txn in
                VStack(spacing: 0) {
                    HStack(spacing: 12) {
                        // Type icon
                        Image(systemName: txnIcon(txn.type_))
                            .foregroundColor(vm.statusColor(txn.status))
                            .frame(width: 34, height: 34)
                            .background(vm.statusColor(txn.status).opacity(0.1))
                            .cornerRadius(8)
                        VStack(alignment: .leading, spacing: 3) {
                            HStack(spacing: 6) {
                                Text(txn.orderId).font(.b360Caption).fontWeight(.bold).foregroundColor(.b360Green)
                                Text("· \(txn.type_)").font(.b360Small).foregroundColor(.b360TextSecondary)
                            }
                            HStack(spacing: 6) {
                                Text(txn.cardType).font(.system(size: 9, weight: .black))
                                    .foregroundColor(.b360Blue).padding(.horizontal, 4).padding(.vertical, 1)
                                    .background(Color.b360Blue.opacity(0.1)).cornerRadius(3)
                                Text("••\(txn.cardLast4)").font(.b360Small).foregroundColor(.b360TextSecondary)
                                if !txn.approvalCode.isEmpty {
                                    Text("✓ \(txn.approvalCode)").font(.b360Small).foregroundColor(.b360Green).fontWeight(.semibold)
                                }
                            }
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("KES \(Int(txn.amount).formatted())").font(.b360Body).fontWeight(.bold)
                            StatusBadge(text: txn.status)
                            Text(txn.date).font(.b360Small).foregroundColor(.b360TextSecondary)
                        }
                    }
                    .padding(.horizontal, 16).padding(.vertical, 12)
                    if txn.id != vm.transactions.last?.id { B360Divider().padding(.leading, 62) }
                }
            }
        }
        .b360Card()
        .padding(.horizontal, 16)
    }

    func txnIcon(_ type_: String) -> String {
        switch type_ {
        case "CAPTURE": return "checkmark.circle.fill"
        case "REFUND":  return "arrow.counterclockwise.circle.fill"
        case "VOID":    return "xmark.circle.fill"
        default:        return "creditcard.fill"
        }
    }
}

// ── Saved Cards View ──────────────────────────────────────────────────────────
struct SavedCardsView: View {
    @ObservedObject var vm: CyberSourceViewModel

    var body: some View {
        VStack(spacing: 12) {
            ForEach(vm.savedCards) { card in
                HStack(spacing: 12) {
                    Image(systemName: "creditcard.fill")
                        .foregroundColor(card.type_ == "VISA" ? .b360Blue : .b360Red)
                        .frame(width: 36, height: 36)
                        .background((card.type_ == "VISA" ? Color.b360Blue : .b360Red).opacity(0.1))
                        .cornerRadius(8)
                    VStack(alignment: .leading, spacing: 3) {
                        HStack {
                            Text(card.type_).font(.system(size: 9, weight: .black))
                                .foregroundColor(card.type_ == "VISA" ? .b360Blue : .b360Red)
                            Text("•••• •••• •••• \(card.last4)").font(.b360Body).fontWeight(.semibold)
                        }
                        Text("\(card.holder) · Exp \(card.expiry)").font(.b360Small).foregroundColor(.b360TextSecondary)
                    }
                    Spacer()
                    if card.isDefault {
                        Text("DEFAULT").font(.b360Small).fontWeight(.bold)
                            .foregroundColor(.b360Green)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(Color.b360GreenBg).cornerRadius(8)
                    }
                }
                .padding(14)
                .b360Card()
            }

            // Info card
            VStack(alignment: .leading, spacing: 8) {
                Label("Zero PCI Scope", systemImage: "lock.shield.fill").font(.b360Caption).fontWeight(.bold).foregroundColor(.b360Green)
                Text("Card numbers are stored securely in CyberSource Token Management Service (TMS). Biashara360 servers never see raw card data.")
                    .font(.b360Small).foregroundColor(.b360TextSecondary)
            }
            .padding(14)
            .background(Color.b360GreenBg)
            .cornerRadius(10)
        }
        .padding(.horizontal, 16)
    }
}
