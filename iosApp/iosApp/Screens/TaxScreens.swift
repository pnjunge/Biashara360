import SwiftUI

// ── Data Models ───────────────────────────────────────────────────────────────

struct TaxRateItem: Identifiable {
    let id: String
    let taxType: String
    let name: String
    let ratePercent: Double
    var isActive: Bool
    let appliesTo: String
    let isInclusive: Bool
    let description: String

    var typeColor: Color {
        switch taxType {
        case "VAT":    return Color(red: 0.11, green: 0.55, blue: 0.20)
        case "TOT":    return Color(red: 0.08, green: 0.40, blue: 0.75)
        case "WHT":    return Color(red: 0.42, green: 0.11, blue: 0.60)
        case "EXCISE": return Color(red: 0.90, green: 0.32, blue: 0.00)
        default:       return Color.gray
        }
    }
    var typeBg: Color { typeColor.opacity(0.12) }
}

struct RemittanceItem: Identifiable {
    let id: String
    let taxType: String
    let period: String
    let taxableAmount: Double
    let taxAmount: Double
    let status: String
    let receiptNumber: String?

    var statusColor: Color {
        switch status {
        case "PAID":  return Color(red: 0.11, green: 0.55, blue: 0.20)
        case "FILED": return Color(red: 0.08, green: 0.40, blue: 0.75)
        default:      return Color(red: 1.0,  green: 0.56, blue: 0.00)
        }
    }
}

// ── Sample Data ───────────────────────────────────────────────────────────────

private let sampleRates = [
    TaxRateItem(id:"1", taxType:"VAT",    name:"Value Added Tax",  ratePercent:16.0, isActive:true,  appliesTo:"PRODUCTS", isInclusive:false, description:"16% VAT on taxable goods & services. Mandatory for >KES 5M annual turnover."),
    TaxRateItem(id:"2", taxType:"TOT",    name:"Turnover Tax",     ratePercent:1.5,  isActive:false, appliesTo:"ALL",      isInclusive:false, description:"1.5% TOT on gross receipts. For businesses KES 1M–5M turnover."),
    TaxRateItem(id:"3", taxType:"WHT",    name:"Withholding Tax",  ratePercent:3.0,  isActive:true,  appliesTo:"SERVICES", isInclusive:false, description:"3% WHT deducted at source on qualifying payments."),
    TaxRateItem(id:"4", taxType:"EXCISE", name:"Excise Duty",      ratePercent:20.0, isActive:false, appliesTo:"PRODUCTS", isInclusive:false, description:"Excise Duty on alcohol, tobacco & specified goods."),
]

private let sampleRemittances = [
    RemittanceItem(id:"1", taxType:"VAT", period:"Feb 2026", taxableAmount:420000, taxAmount:67200, status:"PAID",    receiptNumber:"KRA-2026-02-VAT-001"),
    RemittanceItem(id:"2", taxType:"VAT", period:"Jan 2026", taxableAmount:380000, taxAmount:60800, status:"PAID",    receiptNumber:"KRA-2026-01-VAT-001"),
    RemittanceItem(id:"3", taxType:"WHT", period:"Feb 2026", taxableAmount:45000,  taxAmount:1350,  status:"FILED",   receiptNumber:"KRA-2026-02-WHT-001"),
    RemittanceItem(id:"4", taxType:"VAT", period:"Mar 2026", taxableAmount:0,       taxAmount:0,     status:"PENDING", receiptNumber:nil),
]

let b360Green = Color(red: 0.11, green: 0.55, blue: 0.20)

// ── Main Tax View ─────────────────────────────────────────────────────────────

struct TaxView: View {
    @State private var selectedTab = 0
    let tabs = ["Summary", "Tax Rates", "Calculator", "Remittances"]

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Custom tab picker
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(tabs.indices, id: \.self) { i in
                            Button(action: { selectedTab = i }) {
                                Text(tabs[i])
                                    .font(.system(size: 13, weight: selectedTab == i ? .bold : .medium))
                                    .foregroundColor(selectedTab == i ? b360Green : .secondary)
                                    .padding(.horizontal, 16).padding(.vertical, 10)
                                    .background(selectedTab == i ? b360Green.opacity(0.10) : Color.clear)
                                    .cornerRadius(20)
                            }
                        }
                    }.padding(.horizontal, 12).padding(.vertical, 8)
                }
                .background(Color.white)
                Divider()

                switch selectedTab {
                case 0: TaxSummaryView()
                case 1: TaxRatesView()
                case 2: TaxCalculatorView()
                case 3: TaxRemittancesView()
                default: EmptyView()
                }
            }
            .navigationTitle("Tax Management")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {}) {
                        Image(systemName: "arrow.down.doc.fill").foregroundColor(b360Green)
                    }
                }
            }
        }
    }
}

// ── Summary View ──────────────────────────────────────────────────────────────

struct TaxSummaryView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    TaxKpiCard(title: "VAT Collected",   value: "KES 67,200", sub: "February 2026",        color: b360Green)
                    TaxKpiCard(title: "WHT Collected",   value: "KES 1,800",  sub: "February 2026",        color: Color(red:0.42,green:0.11,blue:0.60))
                    TaxKpiCard(title: "Total Liability", value: "KES 69,000", sub: "All types Feb 2026",   color: Color(red:0.90,green:0.32,blue:0.00))
                    TaxKpiCard(title: "Pending Returns", value: "1",          sub: "Mar VAT due Apr 20",   color: Color(red:1.0,green:0.56,blue:0.00))
                }

                // Effective rate card
                VStack(alignment: .leading, spacing: 10) {
                    Text("Effective Tax Rate").font(.headline)
                    Text("KES 69,000 tax on KES 465,000 revenue").font(.caption).foregroundColor(.secondary)
                    ProgressView(value: 0.148)
                        .progressViewStyle(LinearProgressViewStyle(tint: b360Green))
                        .scaleEffect(x: 1, y: 2, anchor: .center)
                    Text("14.8% effective rate").font(.subheadline).bold().foregroundColor(b360Green)
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.06), radius: 6, y: 2)

                // KRA deadlines
                VStack(alignment: .leading, spacing: 10) {
                    Text("KRA Filing Deadlines").font(.headline)
                    ForEach([
                        ("VAT",    "20th of following month", b360Green),
                        ("TOT",    "20th of following month", Color(red:0.08,green:0.40,blue:0.75)),
                        ("WHT",    "20th of following month", Color(red:0.42,green:0.11,blue:0.60)),
                        ("PAYE",   "9th of following month",  Color.gray),
                    ], id: \.0) { type, deadline, color in
                        HStack {
                            Text(type)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(color)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(color.opacity(0.12)).cornerRadius(6)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Monthly Filing").font(.system(size: 13, weight: .semibold))
                                Text(deadline).font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Image(systemName: "calendar").foregroundColor(color)
                        }
                        .padding(.vertical, 6)
                        if type != "PAYE" { Divider() }
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Tax Rates View ────────────────────────────────────────────────────────────

struct TaxRatesView: View {
    @State private var rates = sampleRates

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Kenya defaults banner
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Kenya Tax Defaults").font(.system(size: 13, weight: .bold)).foregroundColor(b360Green)
                        Text("VAT 16% · TOT 1.5% · WHT 3% · Excise 20%").font(.caption).foregroundColor(Color(red:0.22,green:0.56,blue:0.24))
                    }
                    Spacer()
                    Button("Seed Defaults") {}
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white).padding(.horizontal, 12).padding(.vertical, 7)
                        .background(b360Green).cornerRadius(8)
                }
                .padding(14).background(Color(red:0.91,green:0.96,blue:0.91)).cornerRadius(12)

                ForEach(rates.indices, id: \.self) { i in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text(rates[i].taxType)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(rates[i].typeColor)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(rates[i].typeBg).cornerRadius(6)
                            Text(rates[i].name).font(.system(size: 14, weight: .bold))
                            Spacer()
                            Text("\(rates[i].ratePercent, specifier: "%.1f")%")
                                .font(.system(size: 22, weight: .black))
                                .foregroundColor(rates[i].typeColor)
                        }
                        Text(rates[i].description).font(.caption).foregroundColor(.secondary)
                        HStack {
                            Label(rates[i].appliesTo, systemImage: "tag").font(.caption).foregroundColor(.secondary)
                            Label(rates[i].isInclusive ? "Inclusive" : "Exclusive", systemImage: "percent").font(.caption).foregroundColor(.secondary)
                            Spacer()
                            Toggle("", isOn: Binding(get: { rates[i].isActive }, set: { rates[i].isActive = $0 }))
                                .labelsHidden().tint(b360Green).scaleEffect(0.85)
                        }
                    }
                    .padding(16).background(Color.white).cornerRadius(14)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(rates[i].typeColor.opacity(rates[i].isActive ? 0.3 : 0.1), lineWidth: 2))
                    .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
                    .opacity(rates[i].isActive ? 1.0 : 0.6)
                }

                Button(action: {}) {
                    Label("Add Custom Tax Rate", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity).padding(14)
                        .background(Color.white).cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(b360Green, lineWidth: 1.5))
                        .foregroundColor(b360Green).font(.system(size: 14, weight: .semibold))
                }
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Tax Calculator View ───────────────────────────────────────────────────────

struct TaxCalculatorView: View {
    @State private var amount: String = "10000"
    @State private var selectedIds: Set<String> = ["1"]

    var numAmount: Double { Double(amount) ?? 0 }
    var activeRates: [TaxRateItem] { sampleRates.filter { $0.isActive } }
    var lines: [(rate: TaxRateItem, taxAmount: Double)] {
        activeRates.filter { selectedIds.contains($0.id) }.map { r in
            (r, (numAmount * r.ratePercent / 100 * 100).rounded() / 100)
        }
    }
    var totalTax: Double   { lines.reduce(0) { $0 + $1.taxAmount } }
    var grandTotal: Double { numAmount + totalTax }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                // Amount input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Amount (KES)").font(.system(size: 13, weight: .semibold)).foregroundColor(.secondary)
                    TextField("e.g. 10000", text: $amount)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 20, weight: .bold))
                        .padding(12).background(Color(UIColor.systemGray6)).cornerRadius(10)
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                // Rate selection
                VStack(alignment: .leading, spacing: 10) {
                    Text("Select Tax Rates").font(.headline)
                    ForEach(activeRates) { rate in
                        Button(action: {
                            if selectedIds.contains(rate.id) { selectedIds.remove(rate.id) }
                            else { selectedIds.insert(rate.id) }
                        }) {
                            HStack {
                                Image(systemName: selectedIds.contains(rate.id) ? "checkmark.square.fill" : "square")
                                    .foregroundColor(rate.typeColor)
                                Text(rate.taxType)
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(rate.typeColor)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(rate.typeBg).cornerRadius(5)
                                Text(rate.name).font(.system(size: 13, weight: .medium)).foregroundColor(.primary)
                                Spacer()
                                Text("\(rate.ratePercent, specifier: "%.1f")%")
                                    .font(.system(size: 14, weight: .bold)).foregroundColor(rate.typeColor)
                            }
                            .padding(12)
                            .background(selectedIds.contains(rate.id) ? rate.typeBg : Color(UIColor.systemGray6))
                            .cornerRadius(10)
                        }
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                // Breakdown
                VStack(spacing: 10) {
                    HStack { Text("Subtotal").foregroundColor(.secondary); Spacer()
                        Text("KES \(Int(numAmount).formatted())").fontWeight(.semibold) }
                    ForEach(lines, id: \.rate.id) { item in
                        HStack {
                            Text("\(item.rate.name) (\(item.rate.ratePercent, specifier: "%.1f")%)").foregroundColor(item.rate.typeColor).font(.subheadline)
                            Spacer()
                            Text("+ KES \(Int(item.taxAmount).formatted())").foregroundColor(item.rate.typeColor).fontWeight(.bold)
                        }
                    }
                    Divider()
                    HStack {
                        Text("Total Tax").fontWeight(.bold)
                        Spacer()
                        Text("KES \(Int(totalTax).formatted())").fontWeight(.black).font(.title3).foregroundColor(b360Green)
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                // Grand total
                HStack {
                    Text("Grand Total").font(.title3).fontWeight(.bold).foregroundColor(.white)
                    Spacer()
                    Text("KES \(Int(grandTotal).formatted())").font(.title).fontWeight(.black).foregroundColor(.white)
                }
                .padding(20).background(b360Green).cornerRadius(14)
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Remittances View ──────────────────────────────────────────────────────────

struct TaxRemittancesView: View {
    @State private var filterType = "ALL"
    let filterTypes = ["ALL","VAT","TOT","WHT","EXCISE"]

    var filtered: [RemittanceItem] {
        filterType == "ALL" ? sampleRemittances : sampleRemittances.filter { $0.taxType == filterType }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Filter chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(filterTypes, id: \.self) { type in
                        Button(action: { filterType = type }) {
                            Text(type)
                                .font(.system(size: 12, weight: filterType == type ? .bold : .medium))
                                .foregroundColor(filterType == type ? b360Green : .secondary)
                                .padding(.horizontal, 14).padding(.vertical, 7)
                                .background(filterType == type ? b360Green.opacity(0.1) : Color(UIColor.systemGray5))
                                .cornerRadius(20)
                        }
                    }
                }.padding(.horizontal, 16).padding(.vertical, 8)
            }
            .background(Color.white)
            Divider()

            ScrollView {
                VStack(spacing: 12) {
                    // File period button
                    Button(action: {}) {
                        Label("File New Period", systemImage: "plus.circle.fill")
                            .frame(maxWidth: .infinity).padding(14)
                            .background(b360Green).cornerRadius(12)
                            .foregroundColor(.white).font(.system(size: 14, weight: .semibold))
                    }

                    ForEach(filtered) { r in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text(r.taxType)
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(r.statusColor)
                                    .padding(.horizontal, 10).padding(.vertical, 4)
                                    .background(r.statusColor.opacity(0.12)).cornerRadius(6)
                                Text(r.period).font(.system(size: 14, weight: .bold))
                                Spacer()
                                Text(r.status)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(r.statusColor)
                                    .padding(.horizontal, 12).padding(.vertical, 4)
                                    .background(r.statusColor.opacity(0.12)).cornerRadius(20)
                            }

                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("Taxable Amount").font(.caption).foregroundColor(.secondary)
                                    Text("KES \(Int(r.taxableAmount).formatted())").fontWeight(.semibold)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 3) {
                                    Text("Tax Due").font(.caption).foregroundColor(.secondary)
                                    Text("KES \(Int(r.taxAmount).formatted())").font(.title3).fontWeight(.black).foregroundColor(b360Green)
                                }
                            }

                            if let receipt = r.receiptNumber {
                                Text("KRA Receipt: \(receipt)").font(.caption).foregroundColor(.secondary)
                            }

                            if r.status == "PENDING" {
                                HStack(spacing: 10) {
                                    Button("Mark Filed") {}
                                        .frame(maxWidth: .infinity).padding(10)
                                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(red:0.08,green:0.40,blue:0.75), lineWidth: 1.5))
                                        .foregroundColor(Color(red:0.08,green:0.40,blue:0.75)).font(.system(size: 13, weight: .semibold))
                                    Button("Mark Paid") {}
                                        .frame(maxWidth: .infinity).padding(10)
                                        .background(b360Green).cornerRadius(8)
                                        .foregroundColor(.white).font(.system(size: 13, weight: .semibold))
                                }
                            }
                        }
                        .padding(16).background(Color.white).cornerRadius(14)
                        .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
                    }
                }
                .padding(16)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

struct TaxKpiCard: View {
    let title: String; let value: String; let sub: String; let color: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.caption).foregroundColor(.secondary)
            Text(value).font(.title3).fontWeight(.black).foregroundColor(color)
            Text(sub).font(.caption2).foregroundColor(Color(UIColor.tertiaryLabel))
        }
        .padding(14).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white).cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 5, y: 2)
    }
}
