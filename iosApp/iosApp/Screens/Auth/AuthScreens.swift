import SwiftUI

// ── Login Screen ──────────────────────────────────────────────────────────────
struct LoginView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    // Logo
                    VStack(spacing: 8) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 20)
                                .fill(Color.b360Green)
                                .frame(width: 80, height: 80)
                            Text("B360")
                                .font(.system(size: 22, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                        }
                        Text("Biashara360")
                            .font(.b360Title)
                            .foregroundColor(.b360TextPrimary)
                        Text("Business management for Kenyan traders")
                            .font(.b360Caption)
                            .foregroundColor(.b360TextSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 48)

                    // Form
                    VStack(spacing: 16) {
                        B360TextField(label: "Email", placeholder: "wanjiru@example.com", text: $email,
                                     icon: "envelope", keyboardType: .emailAddress)
                        B360TextField(label: "Password", placeholder: "••••••••", text: $password,
                                     icon: "lock", isSecure: true)

                        if let err = authVM.errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.circle.fill")
                                Text(err)
                            }
                            .font(.b360Caption)
                            .foregroundColor(.b360Red)
                        }

                        B360Button(title: "Sign In", icon: "arrow.right", isLoading: authVM.isLoading) {
                            Task { await authVM.login(email: email, password: password) }
                        }

                        Button("Forgot password?") {}
                            .font(.b360Caption)
                            .foregroundColor(.b360Green)
                    }

                    Spacer()
                    Text("© 2025 Biashara360ERP — Kenya Data Protection Act compliant")
                        .font(.system(size: 10))
                        .foregroundColor(.b360TextSecondary)
                        .padding(.bottom, 24)
                }
                .padding(.horizontal, 24)
            }
            .background(Color.b360Surface.ignoresSafeArea())
        }
        .sheet(isPresented: $authVM.requiresOtp) {
            OtpVerifyView()
                .environmentObject(authVM)
        }
    }
}

// ── OTP Verify Screen ─────────────────────────────────────────────────────────
struct OtpVerifyView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @State private var otp = ""
    @FocusState private var focused: Bool

    var body: some View {
        VStack(spacing: 28) {
            Image(systemName: "message.badge.filled.fill")
                .font(.system(size: 48))
                .foregroundColor(.b360Green)
                .padding(.top, 32)

            VStack(spacing: 8) {
                Text("Verify your identity")
                    .font(.b360Title)
                Text("We sent a 6-digit code via SMS to your registered phone number.")
                    .font(.b360Body)
                    .foregroundColor(.b360TextSecondary)
                    .multilineTextAlignment(.center)
            }

            // OTP input
            TextField("Enter 6-digit code", text: $otp)
                .keyboardType(.numberPad)
                .font(.system(size: 32, weight: .bold, design: .monospaced))
                .multilineTextAlignment(.center)
                .frame(height: 60)
                .background(Color.b360Surface)
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.b360Green, lineWidth: 2))
                .focused($focused)
                .onChange(of: otp) { newVal in
                    if newVal.count > 6 { otp = String(newVal.prefix(6)) }
                    if newVal.count == 6 {
                        Task { await authVM.verifyOtp(code: newVal) }
                    }
                }

            if let err = authVM.errorMessage {
                HStack {
                    Image(systemName: "xmark.circle.fill")
                    Text(err)
                }
                .font(.b360Caption)
                .foregroundColor(.b360Red)
            }

            B360Button(title: "Verify", icon: "checkmark.shield", isLoading: authVM.isLoading) {
                Task { await authVM.verifyOtp(code: otp) }
            }
            .disabled(otp.count < 6)

            Button("Resend code") {}
                .font(.b360Caption)
                .foregroundColor(.b360Green)

            Spacer()
        }
        .padding(.horizontal, 32)
        .background(Color.b360Surface.ignoresSafeArea())
        .onAppear { focused = true }
    }
}
