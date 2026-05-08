import SwiftUI

struct ProfileViewIOS: View {
    @EnvironmentObject private var session: SessionStore
    @State private var username = TokenManager.shared.username ?? ""
    @State private var fullName = TokenManager.shared.fullName ?? ""
    @State private var email = TokenManager.shared.email ?? ""
    @State private var phone = ""
    @State private var message: String?
    @State private var isSaving = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Hồ sơ") {
                    TextField("Username", text: $username)
                    TextField("Họ tên", text: $fullName)
                    TextField("Email", text: $email)
                    TextField("Số điện thoại", text: $phone)
                }

                Section {
                    Button(isSaving ? "Đang lưu..." : "Lưu thay đổi") {
                        Task { await saveProfile() }
                    }
                    .disabled(isSaving)

                    NavigationLink("Đơn hàng của tôi") {
                        OrdersView()
                    }

                    Button("Đăng xuất", role: .destructive) {
                        session.logout()
                    }
                }

                if let message {
                    Section { Text(message).foregroundColor(.secondary) }
                }
            }
            .navigationTitle("Tài khoản")
        }
    }

    private func saveProfile() async {
        isSaving = true
        defer { isSaving = false }
        do {
            try await UserService.shared.updateProfile(
                UpdateProfileRequest(
                    username: username.trimmingCharacters(in: .whitespacesAndNewlines),
                    fullName: fullName.trimmingCharacters(in: .whitespacesAndNewlines),
                    email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                    phone: phone.trimmingCharacters(in: .whitespacesAndNewlines)
                )
            )
            message = "Cập nhật hồ sơ thành công."
        } catch {
            message = error.localizedDescription
        }
    }
}

