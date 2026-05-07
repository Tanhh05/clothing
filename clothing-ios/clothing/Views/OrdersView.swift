import SwiftUI
import Combine

@MainActor
final class OrdersViewModel: ObservableObject {
    @Published var orders: [OrderSummary] = []
    @Published var isLoading = false
    @Published var error: String?

    func load() async {
        isLoading = true
        error = nil
        defer { isLoading = false }
        do {
            let page = try await OrderService.shared.getMyOrders(page: 0, size: 20)
            orders = page.content ?? []
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct OrdersView: View {
    @StateObject private var viewModel = OrdersViewModel()

    var body: some View {
        List {
            if viewModel.isLoading {
                HStack { Spacer(); ProgressView(); Spacer() }
            } else if let error = viewModel.error {
                Text(error).foregroundColor(.red)
            } else if viewModel.orders.isEmpty {
                Text("Chưa có đơn hàng nào.")
            } else {
                ForEach(viewModel.orders) { order in
                    NavigationLink {
                        OrderDetailView(orderId: order.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("#\(order.id)").font(.headline)
                            Text(order.status ?? "-").font(.caption)
                            Text("\(Int(order.totalPrice ?? 0))₫").font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("Đơn hàng")
        .task { await viewModel.load() }
        .refreshable { await viewModel.load() }
    }
}

struct OrderDetailView: View {
    let orderId: Int
    @State private var order: OrderSummary?
    @State private var error: String?
    @State private var isLoading = false

    var body: some View {
        List {
            if isLoading {
                HStack { Spacer(); ProgressView(); Spacer() }
            } else if let error {
                Text(error).foregroundColor(.red)
            } else if let order {
                Section("Thông tin") {
                    Text("Mã đơn: #\(order.id)")
                    Text("Trạng thái: \(order.status ?? "-")")
                    Text("Thanh toán: \(order.paymentMethod ?? "-")")
                    Text("Tổng tiền: \(Int(order.totalPrice ?? 0))₫")
                }
                Section("Sản phẩm") {
                    ForEach(order.items ?? []) { item in
                        VStack(alignment: .leading) {
                            Text(item.productName ?? "Sản phẩm")
                            Text("SL: \(item.quantity ?? 0) - \(Int(item.lineTotal ?? 0))₫")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("Chi tiết đơn")
        .task { await loadDetail() }
    }

    private func loadDetail() async {
        isLoading = true
        defer { isLoading = false }
        do {
            order = try await OrderService.shared.getMyOrderDetail(orderId: orderId)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
