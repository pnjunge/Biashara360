import SwiftUI

// ── KPI Card ──────────────────────────────────────────────────────────────────
struct KpiCard: View {
    let title: String
    let value: String
    let change: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .font(.b360Caption)
                    .foregroundColor(.b360TextSecondary)
                Spacer()
                Image(systemName: icon)
                    .font(.system(size: 16))
                    .foregroundColor(color)
                    .frame(width: 34, height: 34)
                    .background(color.opacity(0.1))
                    .cornerRadius(8)
            }
            Text(value)
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundColor(color)
            Text(change)
                .font(.b360Small)
                .foregroundColor(.b360TextSecondary)
        }
        .padding(16)
        .b360Card()
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
struct SectionHeader: View {
    let title: String
    var actionLabel: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title)
                .font(.b360Headline)
                .foregroundColor(.b360TextPrimary)
            Spacer()
            if let label = actionLabel {
                Button(action: { action?() }) {
                    Text(label)
                        .font(.b360Caption)
                        .foregroundColor(.b360Green)
                }
            }
        }
    }
}

// ── Primary Button ────────────────────────────────────────────────────────────
struct B360Button: View {
    let title: String
    let icon: String?
    var isLoading: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    if let icon = icon {
                        Image(systemName: icon).font(.system(size: 15, weight: .semibold))
                    }
                    Text(title).font(.b360Headline)
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(isLoading ? Color.b360Green.opacity(0.7) : Color.b360Green)
            .cornerRadius(12)
        }
        .disabled(isLoading)
    }
}

// ── Input Field ───────────────────────────────────────────────────────────────
struct B360TextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var icon: String? = nil
    var keyboardType: UIKeyboardType = .default
    var isSecure: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.b360Caption)
                .foregroundColor(.b360TextSecondary)
            HStack {
                if let icon = icon {
                    Image(systemName: icon)
                        .foregroundColor(.b360TextSecondary)
                        .frame(width: 20)
                }
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                        .keyboardType(keyboardType)
                }
            }
            .padding(12)
            .background(Color.b360Surface)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.b360Border, lineWidth: 1))
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var buttonTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: icon)
                .font(.system(size: 52))
                .foregroundColor(.b360Green.opacity(0.4))
            Text(title).font(.b360Headline).foregroundColor(.b360TextPrimary)
            Text(message).font(.b360Body).foregroundColor(.b360TextSecondary).multilineTextAlignment(.center)
            if let btnTitle = buttonTitle {
                B360Button(title: btnTitle, icon: "plus", action: { action?() })
                    .padding(.horizontal, 40)
            }
            Spacer()
        }
        .padding()
    }
}

// ── Row Divider ───────────────────────────────────────────────────────────────
struct B360Divider: View {
    var body: some View {
        Divider().background(Color.b360Border)
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────────
struct AvatarView: View {
    let name: String
    var size: CGFloat = 40
    var color: Color = .b360Green

    var initial: String {
        String(name.prefix(1)).uppercased()
    }

    var body: some View {
        Text(initial)
            .font(.system(size: size * 0.4, weight: .bold))
            .foregroundColor(.white)
            .frame(width: size, height: size)
            .background(color)
            .clipShape(Circle())
    }
}

// ── Alert Card ────────────────────────────────────────────────────────────────
struct AlertCard: View {
    let message: String
    let icon: String
    let color: Color

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.system(size: 16))
            Text(message)
                .font(.b360Caption)
                .foregroundColor(color)
                .fontWeight(.medium)
            Spacer()
        }
        .padding(12)
        .background(color.opacity(0.08))
        .cornerRadius(10)
    }
}
