import SwiftUI

struct CheckoutView: View {
    @EnvironmentObject private var cartStore: CartStore
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var email = TokenManager.shared.email ?? ""
    @State private var phone = ""
    @State private var address = ""
    @State private var note = ""
    @State private var isSubmitting = false
    @State private var message: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Thông tin nhận hàng") {
                    TextField("Họ tên", text: $name)
                    TextField("Email", text: $email)
                    TextField("Số điện thoại", text: $phone)
                    TextField("Địa chỉ", text: $address)
                    TextField("Ghi chú", text: $note)
                }

                Section {
                    Button(isSubmitting ? "Đang xử lý..." : "Đặt hàng") {
                        Task { await placeOrder() }
                    }
                    .disabled(isSubmitting || cartStore.items.isEmpty)
                }

                if let message {
                    Section {
                        Text(message).foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Thanh toán")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Đóng") { dismiss() }
                }
            }
        }
    }

    private func placeOrder() async {
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let payload = CreateOrderRequest(
                paymentMethod: "CASH_ON_DELIVERY",
                address: address,
                phone: phone,
                customerName: name,
                customerEmail: email,
                notes: note.isEmpty ? nil : note
            )
            let body = try JSONEncoder().encode(payload)
            _ = try await APIClient.shared.request(path: "/api/orders", method: "POST", body: body, requiresAuth: true)
            cartStore.clear()
            message = "Đặt hàng thành công."
        } catch {
            message = error.localizedDescription
        }
    }
}

