import SwiftUI

private func formatVND(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.groupingSeparator = "."
    return "\(formatter.string(from: NSNumber(value: value)) ?? "\(value)")đ"
}

struct CartView: View {
    @EnvironmentObject private var cartStore: CartStore
    @State private var voucherCode = ""
    @State private var showCheckout = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    ForEach(cartStore.items) { line in
                        HStack(alignment: .top, spacing: 12) {
                            AsyncImage(url: URL(string: line.imageURL ?? "")) { phase in
                                switch phase {
                                case .success(let image): image.resizable().scaledToFill()
                                default: Rectangle().fill(Color.gray.opacity(0.2))
                                }
                            }
                            .frame(width: 118, height: 132)
                            .clipShape(RoundedRectangle(cornerRadius: 12))

                            VStack(alignment: .leading, spacing: 8) {
                                Text(line.name)
                                    .font(.system(size: 24, weight: .regular, design: .serif))
                                    .lineLimit(2)
                                Text("Màu: \(line.color ?? "-") | Size: \(line.size ?? "-")")
                                    .font(.headline)
                                    .foregroundColor(.secondary)
                                Stepper("SL: \(line.quantity)", value: Binding(
                                    get: { line.quantity },
                                    set: { cartStore.updateQuantity(line, quantity: $0) }
                                ), in: 1...20)
                                Text(formatVND(line.price * line.quantity))
                                    .font(.system(size: 44, weight: .medium, design: .serif))
                            }
                        }
                        .padding(14)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Mã Khuyến Mãi")
                            .font(.title3.weight(.medium))
                        HStack(spacing: 10) {
                            TextField("Nhập mã của bạn", text: $voucherCode)
                                .padding(.horizontal, 14)
                                .frame(height: 58)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            Button("Áp\nDụng") {}
                                .font(.title3.weight(.semibold))
                                .multilineTextAlignment(.center)
                                .frame(width: 130, height: 58)
                                .background(Color.black)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    VStack(spacing: 12) {
                        HStack {
                            Text("Tạm tính")
                            Spacer()
                            Text(formatVND(cartStore.subtotal))
                        }
                        HStack {
                            Text("Phí vận chuyển")
                            Spacer()
                            Text("Miễn phí")
                        }
                        Divider()
                        HStack {
                            Text("Tổng cộng")
                                .font(.system(size: 26, weight: .regular, design: .serif))
                            Spacer()
                            Text(formatVND(cartStore.subtotal))
                                .font(.system(size: 46, weight: .medium, design: .serif))
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Button("THANH TOÁN NGAY") {
                        showCheckout = true
                    }
                    .font(.title3.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 70)
                    .background(Color(red: 0.52, green: 0.39, blue: 0.10))
                    .foregroundColor(.white)
                    .clipShape(Capsule())
                    .disabled(cartStore.items.isEmpty)
                }
                .padding(16)
            }
            .background(Color(white: 0.96))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image(systemName: "line.3.horizontal")
                }
                ToolbarItem(placement: .principal) {
                    Text("GIỎ HÀNG")
                        .font(.system(size: 34, weight: .black, design: .serif))
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Image(systemName: "bell")
                }
            }
            .sheet(isPresented: $showCheckout) {
                CheckoutView()
            }
            .task {
                await cartStore.refreshFromServer()
            }
        }
    }
}
