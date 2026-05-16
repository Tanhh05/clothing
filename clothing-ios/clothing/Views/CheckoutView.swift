import SwiftUI

struct CheckoutView: View {
    @EnvironmentObject private var cartStore: CartStore
    @Environment(\.dismiss) private var dismiss

    @State private var recipientName = TokenManager.shared.fullName ?? ""
    @State private var phone = ""
    @State private var province = ""
    @State private var district = ""
    @State private var ward = ""
    @State private var addressLine = ""
    @State private var voucherCode = ""
    @State private var isSubmitting = false
    @State private var defaultAddressId: Int?
    @State private var addresses: [UserAddress] = []
    @State private var showConfirmOrder = false
    @State private var showResultAlert = false
    @State private var resultTitle = ""
    @State private var resultMessage = ""
    @State private var createdOrderId: Int?

    private var trimmedRecipientName: String { recipientName.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedPhone: String { phone.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedProvince: String { province.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedDistrict: String { district.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedWard: String { ward.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedAddressLine: String { addressLine.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var normalizedVoucherCode: String { voucherCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() }

    private var phoneIsValid: Bool {
        let digitsOnly = trimmedPhone.filter(\.isNumber)
        return digitsOnly.count >= 9 && digitsOnly.count <= 11
    }

    private var formIsValid: Bool {
        !trimmedRecipientName.isEmpty &&
        phoneIsValid &&
        !trimmedProvince.isEmpty &&
        !trimmedDistrict.isEmpty &&
        !trimmedWard.isEmpty &&
        !trimmedAddressLine.isEmpty &&
        !cartStore.items.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Thông tin nhận hàng") {
                    TextField("Họ tên", text: $recipientName)
                    TextField("Số điện thoại", text: $phone)
                        .keyboardType(.numberPad)
                    TextField("Tỉnh / Thành phố", text: $province)
                    TextField("Quận / Huyện", text: $district)
                    TextField("Phường / Xã", text: $ward)
                    TextField("Số nhà, đường", text: $addressLine)
                }

                if !addresses.isEmpty {
                    Section("Địa chỉ đã lưu") {
                        Picker("Chọn địa chỉ", selection: $defaultAddressId) {
                            ForEach(addresses) { addr in
                                Text("\(addr.recipientName) - \(addr.phone)").tag(Optional(addr.id))
                            }
                        }
                        .onChange(of: defaultAddressId) { _, newValue in
                            guard let id = newValue, let addr = addresses.first(where: { $0.id == id }) else { return }
                            applyAddress(addr)
                        }
                    }
                }

                Section("Khuyến mãi") {
                    TextField("Mã voucher", text: $voucherCode)
                        .autocapitalization(.allCharacters)
                }

                Section {
                    Button(isSubmitting ? "Đang xử lý..." : "Đặt hàng") {
                        showConfirmOrder = true
                    }
                    .disabled(isSubmitting || !formIsValid)
                    .tint(.black)
                }

                if !phoneIsValid && !trimmedPhone.isEmpty {
                    Section {
                        Text("Số điện thoại không hợp lệ (9-11 số).")
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }

                Section("Đơn hàng") {
                    HStack {
                        Text("Sản phẩm")
                        Spacer()
                        Text("\(cartStore.items.count)")
                    }
                    HStack {
                        Text("Tạm tính")
                        Spacer()
                        Text("\(cartStore.subtotal)₫").fontWeight(.semibold)
                    }
                }
            }
            .navigationTitle("Thanh toán")
            .scrollContentBackground(.hidden)
            .background(Color(.systemGroupedBackground))
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Đóng") { dismiss() }
                }
            }
            .confirmationDialog(
                "Xác nhận đặt hàng",
                isPresented: $showConfirmOrder,
                titleVisibility: .visible
            ) {
                Button("Xác nhận đặt hàng") {
                    Task { await placeOrder() }
                }
                Button("Hủy", role: .cancel) {}
            } message: {
                Text("Bạn chắc chắn muốn đặt đơn với tổng tạm tính \(cartStore.subtotal)₫?")
            }
            .alert(resultTitle, isPresented: $showResultAlert) {
                Button("OK") {
                    if createdOrderId != nil {
                        dismiss()
                    }
                }
            } message: {
                Text(resultMessage)
            }
            .task {
                await loadAddresses()
            }
        }
    }

    private func placeOrder() async {
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let fullAddress = [addressLine, ward, district, province]
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
                .joined(separator: ", ")
            let payload = CreateOrderRequest(
                paymentMethod: "CASH_ON_DELIVERY",
                address: fullAddress,
                recipientName: trimmedRecipientName.isEmpty ? nil : trimmedRecipientName,
                phone: trimmedPhone.isEmpty ? nil : trimmedPhone,
                province: trimmedProvince.isEmpty ? nil : trimmedProvince,
                district: trimmedDistrict.isEmpty ? nil : trimmedDistrict,
                ward: trimmedWard.isEmpty ? nil : trimmedWard,
                shippingFee: nil,
                voucherCode: normalizedVoucherCode.isEmpty ? nil : normalizedVoucherCode,
                momoRequestType: nil,
                vnpayBankCode: nil
            )
            let order = try await OrderService.shared.createOrder(payload)
            cartStore.clear()
            createdOrderId = order.id
            resultTitle = "Đặt hàng thành công"
            resultMessage = "Mã đơn hàng #\(order.id) đã được tạo."
            showResultAlert = true
        } catch {
            resultTitle = "Đặt hàng thất bại"
            resultMessage = error.localizedDescription
            showResultAlert = true
        }
    }

    private func loadAddresses() async {
        do {
            let fetched = try await UserService.shared.getAddresses()
            addresses = fetched
            if let def = fetched.first(where: { $0.isDefault }) ?? fetched.first {
                defaultAddressId = def.id
                applyAddress(def)
            }
        } catch {
            // Ignore silently, user can input manually.
        }
    }

    private func applyAddress(_ addr: UserAddress) {
        recipientName = addr.recipientName
        phone = addr.phone
        province = addr.province
        district = addr.district
        ward = addr.ward
        addressLine = addr.addressLine
    }
}
