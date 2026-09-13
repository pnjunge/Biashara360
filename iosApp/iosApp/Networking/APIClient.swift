import Foundation
import Security

private struct APIEnvelope<T: Decodable>: Decodable { let success: Bool; let data: T?; let message: String? }
struct LoginResult: Decodable { let userId: String; let requiresOtp: Bool; let otpChannels: [String]; let accessToken: String?; let refreshToken: String? }
struct AuthResult: Decodable { let accessToken: String; let refreshToken: String; let user: AuthenticatedUser }
struct AuthenticatedUser: Decodable { let id: String; let name: String; let email: String; let businessId: String? }

enum APIClientError: LocalizedError {
    case invalidResponse, server(String)
    var errorDescription: String? {
        switch self { case .invalidResponse: return "The server returned an invalid response."; case .server(let message): return message }
    }
}

final class APIClient {
    static let shared = APIClient()
    private let baseURL = URL(string: "https://api.biashara360.co.ke/v1")!

    func login(email: String, password: String) async throws -> LoginResult {
        try await request(path: "auth/login", body: ["email": email, "password": password])
    }

    func verifyOTP(userId: String, otp: String, channel: String) async throws -> AuthResult {
        let result: AuthResult = try await request(path: "auth/verify-otp", body: ["userId": userId, "otp": otp, "channel": channel])
        try SessionStore.save(accessToken: result.accessToken, refreshToken: result.refreshToken)
        return result
    }

    private func request<T: Decodable>(path: String, body: [String: String]) async throws -> T {
        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("ios", forHTTPHeaderField: "X-Client-Platform")
        request.httpBody = try JSONEncoder().encode(body)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIClientError.invalidResponse }
        let envelope = try JSONDecoder().decode(APIEnvelope<T>.self, from: data)
        guard (200..<300).contains(http.statusCode), envelope.success, let value = envelope.data else {
            throw APIClientError.server(envelope.message ?? "Request failed (HTTP \(http.statusCode)).")
        }
        return value
    }
}

enum SessionStore {
    private static let service = Bundle.main.bundleIdentifier ?? "com.app.biashara"
    static var hasSession: Bool { read(account: "accessToken") != nil }
    static func save(accessToken: String, refreshToken: String) throws { try write(accessToken, account: "accessToken"); try write(refreshToken, account: "refreshToken") }
    static func clear() { delete(account: "accessToken"); delete(account: "refreshToken") }

    private static func write(_ value: String, account: String) throws {
        delete(account: account)
        let status = SecItemAdd([kSecClass: kSecClassGenericPassword, kSecAttrService: service, kSecAttrAccount: account, kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, kSecValueData: Data(value.utf8)] as CFDictionary, nil)
        guard status == errSecSuccess else { throw APIClientError.server("Could not securely save the session.") }
    }
    private static func read(account: String) -> Data? {
        var result: CFTypeRef?
        let status = SecItemCopyMatching([kSecClass: kSecClassGenericPassword, kSecAttrService: service, kSecAttrAccount: account, kSecReturnData: true, kSecMatchLimit: kSecMatchLimitOne] as CFDictionary, &result)
        return status == errSecSuccess ? result as? Data : nil
    }
    private static func delete(account: String) { SecItemDelete([kSecClass: kSecClassGenericPassword, kSecAttrService: service, kSecAttrAccount: account] as CFDictionary) }
}
