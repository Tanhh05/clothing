import SwiftUI
import Combine

@MainActor
final class OrdersViewModel: ObservableObject {
    @Published var orders: [OrderSummary] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var productImageById: [Int: String] = [:]

    func load() async {
        isLoading = true
        error = nil
        defer { isLoading = false }
        do {
            let page = try await OrderService.shared.getMyOrders(page: 0, size: 20)
            orders = page.content ?? []
            await loadOrderThumbnails()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func loadOrderThumbnails() async {
        var ids = Set<Int>()
        for order in orders {
            if let firstProductId = order.items?.first?.productId {
                ids.insert(firstProductId)
            }
        }
        for id in ids where productImageById[id] == nil {
            do {
                let product = try await ProductService.shared.getProductAsync(by: String(id))
                if let image = product.mainImageURL {
                    productImageById[id] = image
                }
            } catch {
                continue
            }
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
                        HStack(spacing: 12) {
                            AsyncImage(url: URL(string: viewModel.productImageById[order.items?.first?.productId ?? -1] ?? "")) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Rectangle().fill(Color.gray.opacity(0.2))
                                }
                            }
                            .frame(width: 62, height: 82)
                            .clipShape(RoundedRectangle(cornerRadius: 8))

                            VStack(alignment: .leading, spacing: 4) {
                                Text("#\(order.id)").font(.headline)
                                Text(order.items?.first?.productName ?? "Đơn hàng")
                                    .font(.subheadline)
                                    .lineLimit(1)
                                Text(order.status ?? "-").font(.caption)
                                Text("\(Int(order.totalPrice ?? 0))₫").font(.caption).foregroundColor(.secondary)
                            }
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
    @State private var isMutating = false
    @State private var showCancelConfirm = false
    @State private var showReorderConfirm = false
    @State private var alertText = ""
    @State private var showAlert = false
    @State private var productImageById: [Int: String] = [:]

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
                Section("Thao tác") {
                    Button("Mua lại đơn này") {
                        showReorderConfirm = true
                    }
                    .disabled(isMutating)

                    if (order.status ?? "").uppercased() == "PENDING" {
                        Button("Hủy đơn", role: .destructive) {
                            showCancelConfirm = true
                        }
                        .disabled(isMutating)
                    }
                }
                Section("Sản phẩm") {
                    ForEach(order.items ?? []) { item in
                        HStack(alignment: .top, spacing: 10) {
                            AsyncImage(url: URL(string: productImageById[item.productId ?? -1] ?? "")) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Rectangle().fill(Color.gray.opacity(0.2))
                                }
                            }
                            .frame(width: 56, height: 72)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
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
        }
        .navigationTitle("Chi tiết đơn")
        .task { await loadDetail() }
        .confirmationDialog("Xác nhận hủy đơn?", isPresented: $showCancelConfirm, titleVisibility: .visible) {
            Button("Hủy đơn", role: .destructive) {
                Task { await cancelOrder() }
            }
            Button("Đóng", role: .cancel) {}
        }
        .confirmationDialog("Mua lại đơn này?", isPresented: $showReorderConfirm, titleVisibility: .visible) {
            Button("Xác nhận") {
                Task { await reorder() }
            }
            Button("Đóng", role: .cancel) {}
        }
        .alert("Thông báo", isPresented: $showAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertText)
        }
    }

    private func loadDetail() async {
        isLoading = true
        defer { isLoading = false }
        do {
            order = try await OrderService.shared.getMyOrderDetail(orderId: orderId)
            await loadItemThumbnails()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func loadItemThumbnails() async {
        guard let items = order?.items else { return }
        var ids = Set<Int>()
        for item in items {
            if let pid = item.productId { ids.insert(pid) }
        }
        for id in ids where productImageById[id] == nil {
            do {
                let product = try await ProductService.shared.getProductAsync(by: String(id))
                if let image = product.mainImageURL {
                    productImageById[id] = image
                }
            } catch {
                continue
            }
        }
    }

    private func cancelOrder() async {
        isMutating = true
        defer { isMutating = false }
        do {
            order = try await OrderService.shared.cancelMyOrder(orderId: orderId)
            alertText = "Đã hủy đơn thành công."
            showAlert = true
        } catch {
            alertText = error.localizedDescription
            showAlert = true
        }
    }

    private func reorder() async {
        isMutating = true
        defer { isMutating = false }
        do {
            let newOrder = try await OrderService.shared.reorder(orderId: orderId)
            alertText = "Đã tạo đơn mới #\(newOrder.id)."
            showAlert = true
        } catch {
            alertText = error.localizedDescription
            showAlert = true
        }
    }
}
