import SwiftUI

struct CartView: View {
    @EnvironmentObject private var cartStore: CartStore
    @State private var showCheckout = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if cartStore.items.isEmpty {
                    ContentUnavailableView("Giỏ hàng trống", systemImage: "cart")
                } else {
                    List {
                        ForEach(cartStore.items) { line in
                            HStack(alignment: .top, spacing: 10) {
                                AsyncImage(url: URL(string: line.imageURL ?? "")) { phase in
                                    switch phase {
                                    case .success(let image): image.resizable().scaledToFill()
                                    default: Rectangle().fill(Color.gray.opacity(0.2))
                                    }
                                }
                                .frame(width: 62, height: 82)
                                .clipShape(RoundedRectangle(cornerRadius: 8))

                                VStack(alignment: .leading, spacing: 6) {
                                    Text(line.name).font(.subheadline.weight(.semibold)).lineLimit(2)
                                    Text("\(line.price)₫ x \(line.quantity)").font(.caption).foregroundColor(.secondary)
                                    Stepper("Số lượng: \(line.quantity)", value: Binding(
                                        get: { line.quantity },
                                        set: { cartStore.updateQuantity(line, quantity: $0) }
                                    ), in: 1...99)
                                }
                            }
                        }
                        .onDelete { idx in
                            idx.map { cartStore.items[$0] }.forEach { cartStore.remove($0) }
                        }
                    }

                    VStack(spacing: 10) {
                        HStack {
                            Text("Tạm tính")
                            Spacer()
                            Text("\(cartStore.subtotal)₫").bold()
                        }
                        Button("Thanh toán") {
                            showCheckout = true
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.black)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Giỏ hàng")
            .sheet(isPresented: $showCheckout) {
                CheckoutView()
            }
        }
    }
}

