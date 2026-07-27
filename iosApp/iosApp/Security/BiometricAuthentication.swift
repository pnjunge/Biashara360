import Foundation
import Combine
import LocalAuthentication
import Security

enum BiometricAuthenticationError: Error {
    case missingAccount
    case keychain(OSStatus)
}

@MainActor
final class BiometricAuthentication: ObservableObject {
    @Published private(set) var isAvailable = false
    @Published var isEnabled: Bool {
        didSet { UserDefaults.standard.set(isEnabled, forKey: Self.enabledKey) }
    }

    private static let enabledKey = "biometricLoginEnabled"
    private static let accountKey = "biometricAccountIdentifier"
    private static let service = Bundle.main.bundleIdentifier ?? "com.app.biashara"

    init() {
        isEnabled = UserDefaults.standard.bool(forKey: Self.enabledKey)
        if isEnabled && storedAccountIdentifier() == nil {
            isEnabled = false
        }
        refreshAvailability()
    }

    var displayName: String {
        let context = LAContext()
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
        switch context.biometryType {
        case .faceID: return "Face ID"
        case .touchID: return "Touch ID"
        default: return "Biometrics"
        }
    }

    func refreshAvailability() {
        let context = LAContext()
        isAvailable = context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: nil
        )
        if !isAvailable { isEnabled = false }
    }

    func authenticate(reason: String) async throws {
        let context = LAContext()
        context.localizedCancelTitle = "Use password"
        try await context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: reason
        )
    }

    func enable(accountIdentifier: String) async throws {
        guard !accountIdentifier.isEmpty else {
            throw BiometricAuthenticationError.missingAccount
        }
        try await authenticate(reason: "Confirm your identity to enable biometric login.")
        try storeAccountIdentifier(accountIdentifier)
        isEnabled = true
    }

    func disable() {
        isEnabled = false
        deleteStoredAccountIdentifier()
    }

    func authenticateAndRestoreAccount(reason: String) async throws -> String {
        guard isEnabled, let accountIdentifier = storedAccountIdentifier() else {
            isEnabled = false
            throw BiometricAuthenticationError.missingAccount
        }
        try await authenticate(reason: reason)
        return accountIdentifier
    }

    private func storeAccountIdentifier(_ accountIdentifier: String) throws {
        deleteStoredAccountIdentifier()
        let status = SecItemAdd(
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrService: Self.service,
                kSecAttrAccount: Self.accountKey,
                kSecAttrAccessible: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecValueData: Data(accountIdentifier.utf8),
            ] as CFDictionary,
            nil
        )
        guard status == errSecSuccess else {
            throw BiometricAuthenticationError.keychain(status)
        }
    }

    private func storedAccountIdentifier() -> String? {
        var result: CFTypeRef?
        let status = SecItemCopyMatching(
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrService: Self.service,
                kSecAttrAccount: Self.accountKey,
                kSecReturnData: true,
                kSecMatchLimit: kSecMatchLimitOne,
            ] as CFDictionary,
            &result
        )
        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    private func deleteStoredAccountIdentifier() {
        SecItemDelete(
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrService: Self.service,
                kSecAttrAccount: Self.accountKey,
            ] as CFDictionary
        )
    }
}
