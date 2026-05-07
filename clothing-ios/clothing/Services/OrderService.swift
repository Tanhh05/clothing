import Foundation

final class OrderService {
    static let shared = OrderService()
    private init() {}

    func getMyOrders(page: Int = 0, size: Int = 10) async throws -> PageResponse<OrderSummary> {
        let data = try await APIClient.shared.request(
            path: "/api/orders/my",
            queryItems: [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "size", value: String(size))
            ],
            requiresAuth: true
        )
        return try JSONDecoder().decode(PageResponse<OrderSummary>.self, from: data)
    }

    func getMyOrderDetail(orderId: Int) async throws -> OrderSummary {
        let data = try await APIClient.shared.request(
            path: "/api/orders/my/\(orderId)",
            requiresAuth: true
        )
        return try JSONDecoder().decode(OrderSummary.self, from: data)
    }
}

