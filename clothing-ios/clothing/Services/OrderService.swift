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
        return try APIClient.shared.decodeResponse(PageResponse<OrderSummary>.self, from: data)
    }

    func getMyOrderDetail(orderId: Int) async throws -> OrderSummary {
        let data = try await APIClient.shared.request(
            path: "/api/orders/my/\(orderId)",
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(OrderSummary.self, from: data)
    }

    func createOrder(_ payload: CreateOrderRequest) async throws -> OrderSummary {
        let body = try JSONEncoder().encode(payload)
        let data = try await APIClient.shared.request(
            path: "/api/orders",
            method: "POST",
            body: body,
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(OrderSummary.self, from: data)
    }

    func cancelMyOrder(orderId: Int) async throws -> OrderSummary {
        let data = try await APIClient.shared.request(
            path: "/api/orders/my/\(orderId)/cancel",
            method: "PATCH",
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(OrderSummary.self, from: data)
    }

    func reorder(orderId: Int) async throws -> OrderSummary {
        let data = try await APIClient.shared.request(
            path: "/api/orders/my/\(orderId)/reorder",
            method: "POST",
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(OrderSummary.self, from: data)
    }
}
