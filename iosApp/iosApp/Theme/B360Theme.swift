import SwiftUI

// ── Brand Colors ──────────────────────────────────────────────────────────────
extension Color {
    static let b360Green      = Color(hex: "#1B8B34")
    static let b360GreenLight = Color(hex: "#4CAF63")
    static let b360GreenBg    = Color(hex: "#EBF7EE")
    static let b360Amber      = Color(hex: "#FF8C00")
    static let b360AmberBg    = Color(hex: "#FFF4E0")
    static let b360Red        = Color(hex: "#D32F2F")
    static let b360RedBg      = Color(hex: "#FDECEA")
    static let b360Blue       = Color(hex: "#1565C0")
    static let b360BlueBg     = Color(hex: "#E8F0FE")
    static let b360Surface    = Color(hex: "#F4F7F5")
    static let b360Card       = Color.white
    static let b360TextPrimary   = Color(hex: "#1C1C1E")
    static let b360TextSecondary = Color(hex: "#6B7280")
    static let b360Border     = Color(hex: "#E5E7EB")
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(.sRGB, red: Double(r)/255, green: Double(g)/255, blue: Double(b)/255, opacity: Double(a)/255)
    }
}

// ── Typography ────────────────────────────────────────────────────────────────
extension Font {
    static let b360Title    = Font.system(size: 22, weight: .bold, design: .rounded)
    static let b360Headline = Font.system(size: 17, weight: .semibold)
    static let b360Body     = Font.system(size: 15, weight: .regular)
    static let b360Caption  = Font.system(size: 13, weight: .regular)
    static let b360Small    = Font.system(size: 11, weight: .medium)
}

// ── View Modifiers ────────────────────────────────────────────────────────────
struct B360CardStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color.b360Card)
            .cornerRadius(14)
            .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }
}

extension View {
    func b360Card() -> some View { modifier(B360CardStyle()) }
}

// ── Status Badge ──────────────────────────────────────────────────────────────
struct StatusBadge: View {
    let text: String

    var color: Color {
        switch text.uppercased() {
        case "PAID":      return .b360Green
        case "PENDING":   return .b360Amber
        case "COD":       return .b360Blue
        case "FAILED":    return .b360Red
        case "LOW":       return .b360Amber
        case "OUT":       return .b360Red
        case "DELIVERED": return .b360Green
        case "SHIPPED":   return .b360Blue
        default:          return .b360TextSecondary
        }
    }

    var body: some View {
        Text(text)
            .font(.b360Small)
            .fontWeight(.semibold)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(color.opacity(0.12))
            .foregroundColor(color)
            .cornerRadius(20)
    }
}
