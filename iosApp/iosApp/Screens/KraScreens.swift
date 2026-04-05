import SwiftUI

// ── Data ──────────────────────────────────────────────────────────────────────

struct EtimsInvoiceItem: Identifiable {
    let id: String; let invoiceNumber: String; let etimsNumber: String?
    let status: String; let taxableAmount: Double; let taxAmount: Double
    let totalAmount: Double; let submittedAt: String?

    var statusColor: Color {
        switch status {
        case "TRANSMITTED": return Color(red:0.11,green:0.55,blue:0.20)
        case "PENDING":     return Color(red:1.0,green:0.56,blue:0.0)
        default:            return Color(red:0.78,green:0.16,blue:0.16)
        }
    }
    var statusLabel: String {
        switch status {
        case "TRANSMITTED": return "Transmitted"
        case "PENDING":     return "Pending"
        default:            return "Error"
        }
    }
}

struct TaxReturnItem: Identifiable {
    let id: String; let returnType: String; let periodLabel: String
    let dueDate: String; let status: String; let taxAmount: Double
    let ackNo: String?

    var typeColor: Color {
        switch returnType {
        case "VAT3": return Color(red:0.11,green:0.55,blue:0.20)
        case "TOT":  return Color(red:0.08,green:0.40,blue:0.75)
        case "WHT":  return Color(red:0.42,green:0.11,blue:0.60)
        default:     return Color.gray
        }
    }
    var statusColor: Color {
        switch status {
        case "ACKNOWLEDGED": return Color(red:0.11,green:0.55,blue:0.20)
        case "SUBMITTED":    return Color(red:1.0,green:0.56,blue:0.0)
        case "GENERATED":    return Color(red:0.08,green:0.40,blue:0.75)
        default:             return Color.gray
        }
    }
}

private let sampleEtims = [
    EtimsInvoiceItem(id:"1", invoiceNumber:"INV-2026-0147", etimsNumber:"NS00000147", status:"TRANSMITTED", taxableAmount:12500, taxAmount:2000, totalAmount:14500, submittedAt:"Today 14:22"),
    EtimsInvoiceItem(id:"2", invoiceNumber:"INV-2026-0146", etimsNumber:"NS00000146", status:"TRANSMITTED", taxableAmount:8600,  taxAmount:1376, totalAmount:9976,  submittedAt:"Today 11:05"),
    EtimsInvoiceItem(id:"3", invoiceNumber:"INV-2026-0145", etimsNumber:nil,           status:"ERROR",       taxableAmount:3200,  taxAmount:512,  totalAmount:3712,  submittedAt:nil),
    EtimsInvoiceItem(id:"4", invoiceNumber:"INV-2026-0144", etimsNumber:"NS00000144", status:"TRANSMITTED", taxableAmount:21000, taxAmount:3360, totalAmount:24360, submittedAt:"Yesterday"),
    EtimsInvoiceItem(id:"5", invoiceNumber:"INV-2026-0143", etimsNumber:nil,           status:"PENDING",     taxableAmount:5500,  taxAmount:880,  totalAmount:6380,  submittedAt:nil),
]
private let sampleKraReturns = [
    TaxReturnItem(id:"1", returnType:"VAT3", periodLabel:"Mar 2026", dueDate:"2026-04-20", status:"GENERATED",    taxAmount:71200, ackNo:nil),
    TaxReturnItem(id:"2", returnType:"VAT3", periodLabel:"Feb 2026", dueDate:"2026-03-20", status:"SUBMITTED",    taxAmount:67200, ackNo:"ACK202602VAT001"),
    TaxReturnItem(id:"3", returnType:"VAT3", periodLabel:"Jan 2026", dueDate:"2026-02-20", status:"ACKNOWLEDGED", taxAmount:60800, ackNo:"ACK202601VAT001"),
    TaxReturnItem(id:"4", returnType:"WHT",  periodLabel:"Feb 2026", dueDate:"2026-03-20", status:"SUBMITTED",    taxAmount:1350,  ackNo:"ACK202602WHT001"),
]

// ── Main KRA View ─────────────────────────────────────────────────────────────

struct KraView: View {
    @State private var selectedTab = 0
    let tabs = ["Compliance", "eTIMS", "Returns", "Setup"]

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
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
                }.background(Color.white)
                Divider()

                switch selectedTab {
                case 0: KraComplianceView()
                case 1: KraEtimsView()
                case 2: KraReturnsView()
                case 3: KraSetupView()
                default: EmptyView()
                }
            }
            .navigationTitle("KRA iTax")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Link(destination: URL(string: "https://itax.kra.go.ke")!) {
                        Label("iTax Portal", systemImage: "arrow.up.right.square")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(b360Green)
                    }
                }
            }
        }
    }
}

// ── Compliance View ───────────────────────────────────────────────────────────

struct KraComplianceView: View {
    let score = 82

    var scoreColor: Color { score >= 80 ? b360Green : score >= 50 ? Color(red:1,green:0.56,blue:0) : Color(red:0.78,green:0.16,blue:0.16) }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                // Score card
                HStack(spacing: 20) {
                    ZStack {
                        Circle().stroke(Color(UIColor.systemGray5), lineWidth: 8).frame(width: 80, height: 80)
                        Circle().trim(from: 0, to: CGFloat(score) / 100)
                            .stroke(scoreColor, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .frame(width: 80, height: 80)
                            .rotationEffect(.degrees(-90))
                        VStack(spacing: 0) {
                            Text("\(score)").font(.system(size: 22, weight: .black)).foregroundColor(scoreColor)
                            Text("/100").font(.system(size: 9)).foregroundColor(.secondary)
                        }
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Compliance Score").font(.headline)
                        Text("✅ Good standing").font(.caption).foregroundColor(.secondary)
                        HStack(spacing: 6) {
                            KraTagBadge("VAT Registered", color: b360Green)
                            KraTagBadge("eTIMS Active", color: Color(red:0.08,green:0.40,blue:0.75))
                        }
                    }
                    Spacer()
                }
                .padding(20).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.06), radius: 6, y: 2)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    KraKpiCard(title: "KRA PIN", value: "P051234567X", sub: "Taxpayer ID", color: b360Green)
                    KraKpiCard(title: "eTIMS Rate", value: "94%", sub: "Invoices transmitted", color: Color(red:0.08,green:0.40,blue:0.75))
                }

                // Action items
                VStack(alignment: .leading, spacing: 2) {
                    Text("Action Items").font(.headline).padding(.bottom, 6)
                    ForEach([
                        "📅 March 2026 VAT3 due 20 Apr. Generate and upload now.",
                        "📊 94% of invoices transmitted. Retry 3 failed invoices."
                    ], id: \.self) { rec in
                        HStack {
                            Text(rec).font(.system(size: 13)).foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary).font(.caption)
                        }
                        .padding(.vertical, 10)
                        Divider()
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                // Filing guide
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("📋 iTax Filing Guide").font(.system(size: 14, weight: .bold)).foregroundColor(Color(red:0.08,green:0.40,blue:0.75))
                        Spacer()
                        Link("Open iTax →", destination: URL(string: "https://itax.kra.go.ke")!)
                            .font(.system(size: 12)).foregroundColor(Color(red:0.08,green:0.40,blue:0.75))
                    }
                    ForEach([
                        ("1.", "Generate return (VAT3 / TOT / WHT)"),
                        ("2.", "Download the KRA-format CSV"),
                        ("3.", "Log in at itax.kra.go.ke → Returns → File Returns"),
                        ("4.", "Upload CSV and submit"),
                        ("5.", "Paste acknowledgement number back here"),
                    ], id: \.0) { num, step in
                        HStack(alignment: .top, spacing: 10) {
                            Text(num).font(.system(size: 13, weight: .bold)).foregroundColor(Color(red:0.08,green:0.40,blue:0.75)).frame(width: 20)
                            Text(step).font(.system(size: 13)).foregroundColor(.primary)
                        }
                    }
                }
                .padding(16).background(Color(red:0.97,green:0.98,blue:1.0)).cornerRadius(14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(red:0.77,green:0.79,blue:0.91), lineWidth: 1))
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── eTIMS View ────────────────────────────────────────────────────────────────

struct KraEtimsView: View {
    @State private var invoices = sampleEtims
    @State private var filterAll = true

    var errors: Int { invoices.filter { $0.status == "ERROR" || $0.status == "PENDING" }.count }
    var transmitted: Int { invoices.filter { $0.status == "TRANSMITTED" }.count }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    KraKpiCard(title: "Transmitted", value: "\(transmitted)", sub: "Signed by KRA", color: b360Green)
                    KraKpiCard(title: "Failed", value: "\(errors)", sub: "Need retry", color: errors > 0 ? Color(red:0.78,green:0.16,blue:0.16) : Color.gray)
                }

                // What is eTIMS
                VStack(alignment: .leading, spacing: 6) {
                    Text("What is KRA eTIMS?").font(.system(size: 13, weight: .bold)).foregroundColor(b360Green)
                    Text("Every sale must be transmitted to KRA in real-time. Each invoice gets a unique KRA number and QR code for the customer's receipt, verifiable at etims.kra.go.ke. Mandatory for VAT-registered businesses from January 2024.")
                        .font(.caption).foregroundColor(Color(red:0.22,green:0.56,blue:0.24)).lineSpacing(3)
                }
                .padding(14).background(Color(red:0.91,green:0.96,blue:0.91)).cornerRadius(12)

                if errors > 0 {
                    Button(action: {
                        invoices = invoices.map { inv in
                            (inv.status == "ERROR" || inv.status == "PENDING")
                                ? EtimsInvoiceItem(id: inv.id, invoiceNumber: inv.invoiceNumber, etimsNumber: "NS00000\(inv.id)X", status: "TRANSMITTED", taxableAmount: inv.taxableAmount, taxAmount: inv.taxAmount, totalAmount: inv.totalAmount, submittedAt: "Just now")
                                : inv
                        }
                    }) {
                        Label("Retry \(errors) Failed Invoice\(errors > 1 ? "s" : "")", systemImage: "arrow.clockwise")
                            .frame(maxWidth: .infinity).padding(14)
                            .background(Color(red:0.78,green:0.16,blue:0.16)).cornerRadius(12)
                            .foregroundColor(.white).font(.system(size: 14, weight: .semibold))
                    }
                }

                ForEach(invoices) { inv in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Circle().fill(inv.statusColor).frame(width: 8, height: 8)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(inv.invoiceNumber).font(.system(size: 13, weight: .bold, design: .monospaced))
                                Text(inv.etimsNumber != nil ? "KRA: \(inv.etimsNumber!)" : "Awaiting KRA number")
                                    .font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text(inv.statusLabel)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(inv.statusColor)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(inv.statusColor.opacity(0.12)).cornerRadius(20)
                        }
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("VAT").font(.caption).foregroundColor(.secondary)
                                Text("KES \(Int(inv.taxAmount).formatted())").font(.subheadline).bold().foregroundColor(b360Green)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("Total").font(.caption).foregroundColor(.secondary)
                                Text("KES \(Int(inv.totalAmount).formatted())").font(.title3).bold()
                            }
                        }
                        if let sent = inv.submittedAt {
                            Text("Transmitted: \(sent)").font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .padding(16).background(Color.white).cornerRadius(14)
                    .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
                }
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Returns View ──────────────────────────────────────────────────────────────

struct KraReturnsView: View {
    @State private var returns = sampleKraReturns
    @State private var ackInputs: [String: String] = [:]

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Button(action: {}) {
                    Label("Generate New Return", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity).padding(14)
                        .background(b360Green).cornerRadius(12)
                        .foregroundColor(.white).font(.system(size: 14, weight: .semibold))
                }

                ForEach(returns) { r in
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text(r.returnType)
                                .font(.system(size: 11, weight: .bold)).foregroundColor(r.typeColor)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(r.typeColor.opacity(0.12)).cornerRadius(6)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(r.periodLabel).font(.system(size: 14, weight: .bold))
                                Text("Due \(r.dueDate)").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text(r.status)
                                .font(.system(size: 11, weight: .bold)).foregroundColor(r.statusColor)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(r.statusColor.opacity(0.12)).cornerRadius(20)
                        }

                        HStack {
                            Text("Tax Payable").foregroundColor(.secondary)
                            Spacer()
                            Text("KES \(Int(r.taxAmount).formatted())")
                                .font(.title3).fontWeight(.black).foregroundColor(r.typeColor)
                        }

                        if let ack = r.ackNo {
                            Text("iTax Ack: \(ack)").font(.system(size: 11, design: .monospaced)).foregroundColor(.secondary)
                        }

                        HStack(spacing: 8) {
                            Button(action: {}) {
                                Label("CSV", systemImage: "arrow.down.circle")
                                    .font(.system(size: 12, weight: .semibold)).foregroundColor(r.typeColor)
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(r.typeColor, lineWidth: 1.5))
                            }
                            Link(destination: URL(string: "https://itax.kra.go.ke")!) {
                                Label("Upload on iTax", systemImage: "arrow.up.right.square")
                                    .frame(maxWidth: .infinity).font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(.white).padding(.vertical, 8)
                                    .background(r.typeColor).cornerRadius(8)
                            }
                        }
                    }
                    .padding(16).background(Color.white).cornerRadius(14)
                    .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
                }
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Setup View ────────────────────────────────────────────────────────────────

struct KraSetupView: View {
    @State private var pin = "P051234567X"
    @State private var sdcId = "SDCK2024001"
    @State private var env = "sandbox"
    @State private var saved = false

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if env == "production" {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill").foregroundColor(Color(red:1,green:0.56,blue:0))
                        Text("Production mode: All invoices will be sent to KRA's live eTIMS and are legally binding.")
                            .font(.caption).foregroundColor(Color(red:0.6,green:0.35,blue:0))
                    }
                    .padding(12).background(Color(red:1,green:0.95,blue:0.88)).cornerRadius(10)
                }

                // KRA Profile
                VStack(alignment: .leading, spacing: 12) {
                    Label("KRA Taxpayer Profile", systemImage: "shield.fill").font(.headline).foregroundColor(b360Green)
                    VStack(alignment: .leading, spacing: 5) {
                        Text("KRA PIN *").font(.caption).fontWeight(.semibold).foregroundColor(.secondary)
                        TextField("P051234567X", text: $pin)
                            .textCase(.uppercase).keyboardType(.asciiCapable)
                            .font(.system(size: 16, weight: .bold, design: .monospaced))
                            .padding(12).background(Color(UIColor.systemGray6)).cornerRadius(10)
                        Text("Format: letter + 9 digits + letter").font(.caption2).foregroundColor(.secondary)
                    }
                    VStack(alignment: .leading, spacing: 5) {
                        Text("Environment").font(.caption).fontWeight(.semibold).foregroundColor(.secondary)
                        HStack(spacing: 8) {
                            ForEach([("sandbox","Sandbox"),("production","Production")], id: \.0) { val, label in
                                let sel = env == val
                                let col = val == "production" ? Color(red:0.9,green:0.32,blue:0) : b360Green
                                Button(action: { env = val }) {
                                    Text(label).frame(maxWidth: .infinity)
                                        .font(.system(size: 12, weight: sel ? .bold : .medium))
                                        .foregroundColor(sel ? col : .secondary)
                                        .padding(.vertical, 10)
                                        .background(sel ? col.opacity(0.1) : Color(UIColor.systemGray6))
                                        .cornerRadius(8)
                                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(sel ? col : Color.clear, lineWidth: 2))
                                }
                            }
                        }
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                // eTIMS Device
                VStack(alignment: .leading, spacing: 12) {
                    Label("eTIMS Virtual Device", systemImage: "wifi").font(.headline).foregroundColor(Color(red:0.08,green:0.40,blue:0.75))
                    Text("Register at etims.kra.go.ke to get your SDC ID and serial number.").font(.caption).foregroundColor(.secondary)
                    VStack(alignment: .leading, spacing: 5) {
                        Text("SDC ID").font(.caption).fontWeight(.semibold).foregroundColor(.secondary)
                        TextField("From KRA eTIMS portal", text: $sdcId)
                            .font(.system(size: 14, design: .monospaced))
                            .padding(12).background(Color(UIColor.systemGray6)).cornerRadius(10)
                    }
                    Button(action: {}) {
                        Label("Initialise Device with KRA", systemImage: "bolt.fill")
                            .frame(maxWidth: .infinity).padding(12)
                            .background(Color(red:0.08,green:0.40,blue:0.75)).cornerRadius(10)
                            .foregroundColor(.white).font(.system(size: 13, weight: .semibold))
                    }
                }
                .padding(16).background(Color.white).cornerRadius(14)
                .shadow(color: .black.opacity(0.05), radius: 5, y: 2)

                Button(action: { saved = true; DispatchQueue.main.asyncAfter(deadline: .now() + 2) { saved = false } }) {
                    Label(saved ? "Saved!" : "Save KRA Profile", systemImage: saved ? "checkmark.circle.fill" : "square.and.arrow.down")
                        .frame(maxWidth: .infinity).padding(14)
                        .background(b360Green).cornerRadius(12)
                        .foregroundColor(.white).font(.system(size: 15, weight: .bold))
                }
            }
            .padding(16)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

struct KraKpiCard: View {
    let title: String; let value: String; let sub: String; let color: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.caption).foregroundColor(.secondary)
            Text(value).font(.headline).fontWeight(.black).foregroundColor(color)
            Text(sub).font(.caption2).foregroundColor(Color(UIColor.tertiaryLabel))
        }
        .padding(14).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white).cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

struct KraTagBadge: View {
    let text: String; let color: Color
    init(_ text: String, color: Color) { self.text = text; self.color = color }
    var body: some View {
        Text(text).font(.system(size: 10, weight: .bold)).foregroundColor(color)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(color.opacity(0.12)).cornerRadius(20)
    }
}
