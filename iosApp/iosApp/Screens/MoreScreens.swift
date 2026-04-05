import SwiftUI

// ── Expenses ──────────────────────────────────────────────────────────────────
struct ExpensesView: View {
    @StateObject private var vm = ExpensesViewModel()
    @State private var showAdd = false

    func catColor(_ c: String) -> Color {
        switch c {
        case "ADVERTISING": return .b360Blue
        case "RENT": return .b360Red
        case "STOCK_PURCHASE": return .b360Green
        default: return .b360Amber
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Summary cards
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    StatTile(title: "Total Month", value: "KES\n\(Int(vm.totalThisMonth/1000))K", color: .b360Red)
                    StatTile(title: "Stock Purchase", value: "KES 45K", color: .b360Green)
                    StatTile(title: "Advertising", value: "KES 8.5K", color: .b360Blue)
                    StatTile(title: "Operations", value: "KES 12K", color: .b360Amber)
                }
                .padding(.horizontal, 16)

                // List
                VStack(spacing: 0) {
                    SectionHeader(title: "This Month")
                        .padding(.horizontal, 16)
                        .padding(.bottom, 8)

                    ForEach(vm.expenses) { expense in
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(catColor(expense.category))
                                .frame(width: 4, height: 40)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(expense.description).font(.b360Body).fontWeight(.medium)
                                Text(expense.category.replacingOccurrences(of: "_", with: " "))
                                    .font(.b360Small).foregroundColor(catColor(expense.category))
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 3) {
                                Text("KES \(Int(expense.amount).formatted())")
                                    .font(.b360Caption).fontWeight(.bold).foregroundColor(.b360Red)
                                Text(expense.date).font(.b360Small).foregroundColor(.b360TextSecondary)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        if expense.id != vm.expenses.last?.id { B360Divider().padding(.leading, 16) }
                    }
                }
                .b360Card()
                .padding(.horizontal, 16)

                B360Button(title: "Add Expense", icon: "plus") { showAdd = true }
                    .padding(.horizontal, 16)
            }
            .padding(.top, 12)
        }
        .background(Color.b360Surface)
        .navigationTitle("Expenses & Profit")
    }
}

// ── Payments ──────────────────────────────────────────────────────────────────
struct PaymentsView: View {
    @StateObject private var vm = PaymentsViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    StatTile(title: "Collected", value: "KES\n\(Int(vm.totalCollected/1000))K", color: .b360Green)
                    StatTile(title: "Unreconciled", value: "\(vm.unreconciled.count) txns", color: .b360Amber)
                }
                .padding(.horizontal, 16)

                if !vm.unreconciled.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "Needs Reconciliation")
                        ForEach(vm.unreconciled) { p in
                            PaymentRow(payment: p, showMatchButton: true)
                        }
                    }
                    .padding(.horizontal, 16)
                }

                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(title: "All Transactions")
                    VStack(spacing: 0) {
                        ForEach(vm.payments) { payment in
                            PaymentRow(payment: payment, showMatchButton: false)
                            if payment.id != vm.payments.last?.id { B360Divider().padding(.leading, 16) }
                        }
                    }
                    .b360Card()
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 12)
        }
        .background(Color.b360Surface)
        .navigationTitle("Payments / Mpesa")
    }
}

struct PaymentRow: View {
    let payment: PaymentsViewModel.Payment
    let showMatchButton: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "phone.fill")
                .foregroundColor(.b360Green)
                .frame(width: 32, height: 32)
                .background(Color.b360GreenBg)
                .cornerRadius(8)

            VStack(alignment: .leading, spacing: 3) {
                Text(payment.transactionCode)
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .foregroundColor(.b360Green)
                Text(payment.payerName).font(.b360Body).fontWeight(.medium)
                Text("\(payment.method) · \(payment.date)")
                    .font(.b360Small).foregroundColor(.b360TextSecondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 3) {
                Text("KES \(Int(payment.amount).formatted())")
                    .font(.b360Caption).fontWeight(.bold)
                StatusBadge(text: payment.reconciled ? "MATCHED" : "PENDING")
            }
        }
        .padding(showMatchButton ? 12 : 0)
        .padding(.vertical, showMatchButton ? 0 : 12)
        .padding(.horizontal, showMatchButton ? 0 : 16)
        .background(showMatchButton ? Color.b360AmberBg : Color.clear)
        .cornerRadius(showMatchButton ? 10 : 0)
    }
}

// ── Reports ───────────────────────────────────────────────────────────────────
struct ReportsView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // P&L Summary Card
                VStack(alignment: .leading, spacing: 12) {
                    Text("Profit & Loss — March 2025")
                        .font(.b360Headline)
                    B360Divider()
                    PnlRow(label: "Total Revenue",      value: "KES 145,650", color: .b360Green)
                    PnlRow(label: "Cost of Goods",      value: "KES 72,000",  color: .b360Red)
                    PnlRow(label: "Gross Profit",       value: "KES 73,650",  color: .b360Green, bold: true)
                    PnlRow(label: "Total Expenses",     value: "KES 35,450",  color: .b360Red)
                    B360Divider()
                    PnlRow(label: "Net Profit",         value: "KES 38,200",  color: .b360Green, bold: true, large: true)
                    PnlRow(label: "Net Margin",         value: "26.2%",       color: .b360Blue)
                }
                .padding(16)
                .b360Card()
                .padding(.horizontal, 16)

                // Expense breakdown
                VStack(alignment: .leading, spacing: 12) {
                    Text("Expense Breakdown").font(.b360Headline)
                    ForEach([
                        ("Stock Purchase", 0.69, Color.b360Green, "KES 45,000"),
                        ("Rent",           0.23, Color.b360Red,   "KES 15,000"),
                        ("Advertising",    0.13, Color.b360Blue,  "KES 8,500"),
                        ("Delivery",       0.05, Color.b360Amber, "KES 3,200"),
                        ("Packaging",      0.02, Color.gray,      "KES 1,200"),
                    ], id: \.0) { item in
                        VStack(spacing: 5) {
                            HStack {
                                Text(item.0).font(.b360Caption)
                                Spacer()
                                Text(item.3).font(.b360Caption).fontWeight(.semibold)
                            }
                            ProgressView(value: item.1)
                                .tint(item.2)
                        }
                    }
                }
                .padding(16)
                .b360Card()
                .padding(.horizontal, 16)

                // Export actions
                VStack(spacing: 10) {
                    ExportButton(title: "Export as PDF",      icon: "doc.fill",      color: .b360Red)
                    ExportButton(title: "Export as Excel",    icon: "tablecells",    color: .b360Green)
                    ExportButton(title: "Share via WhatsApp", icon: "message.fill",  color: .b360Blue)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 12)
        }
        .background(Color.b360Surface)
        .navigationTitle("Reports")
    }
}

struct PnlRow: View {
    let label, value: String
    let color: Color
    var bold: Bool = false
    var large: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .font(large ? .b360Headline : .b360Body)
                .fontWeight(bold ? .semibold : .regular)
            Spacer()
            Text(value)
                .font(large ? .b360Headline : .b360Body)
                .fontWeight(bold ? .bold : .semibold)
                .foregroundColor(color)
        }
    }
}

struct ExportButton: View {
    let title: String
    let icon: String
    let color: Color

    var body: some View {
        Button {
        } label: {
            HStack {
                Image(systemName: icon).foregroundColor(color)
                Text(title).fontWeight(.medium)
                Spacer()
                Image(systemName: "arrow.right").foregroundColor(.b360TextSecondary)
            }
            .padding(14)
            .background(Color.b360Card)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border))
        }
        .foregroundColor(.b360TextPrimary)
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────
struct SettingsView: View {
    @State private var twoFA = true
    @State private var smsAlerts = true
    @State private var emailNotifs = false
    @State private var language = "English"

    var body: some View {
        Form {
            Section("Business Profile") {
                LabeledContent("Business Name", value: "Wanjiru's Fashion")
                LabeledContent("Owner Phone",   value: "+254 712 345 678")
                LabeledContent("Business Type", value: "Retail")
                LabeledContent("Mpesa Paybill", value: "174379")
            }

            Section("Security") {
                Toggle("Two-Factor Auth (2FA)", isOn: $twoFA)
                    .tint(.b360Green)
                LabeledContent("2FA Method", value: "SMS OTP")
            }

            Section("Notifications") {
                Toggle("SMS Alerts", isOn: $smsAlerts).tint(.b360Green)
                Toggle("Email Notifications", isOn: $emailNotifs).tint(.b360Green)
            }

            Section("Preferences") {
                Picker("Language", selection: $language) {
                    Text("English").tag("English")
                    Text("Kiswahili").tag("Kiswahili")
                }
            }

            Section("Subscription") {
                LabeledContent("Current Plan", value: "Freemium")
                Button("Upgrade to Premium →") {}
                    .foregroundColor(.b360Green)
            }

            Section("About") {
                LabeledContent("Version", value: "1.0.0")
                LabeledContent("Platform", value: "iOS · Kotlin Multiplatform")
                Link("Privacy Policy", destination: URL(string: "https://biashara360.co.ke/privacy")!)
                Link("Terms of Service", destination: URL(string: "https://biashara360.co.ke/terms")!)
            }
        }
        .navigationTitle("Settings")
    }
}
