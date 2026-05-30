import Foundation

final class CartService {
    static let shared = CartService()
    private init() {}

    func getMyCart() async throws -> CartResponse {
        let data = try await APIClient.shared.request(path: "/api/cart", requiresAuth: true)
        return try APIClient.shared.decodeResponse(CartResponse.self, from: data)
    }

    func addItem(variantId: Int, quantity: Int = 1) async throws -> CartResponse {
        let req = AddCartItemRequest(variantId: variantId, quantity: quantity)
        let body = try JSONEncoder().encode(req)
        let data = try await APIClient.shared.request(
            path: "/api/cart/items",
            method: "POST",
            body: body,
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(CartResponse.self, from: data)
    }

    func updateItem(cartItemId: Int, quantity: Int) async throws -> CartResponse {
        let req = UpdateCartItemRequest(quantity: quantity)
        let body = try JSONEncoder().encode(req)
        let data = try await APIClient.shared.request(
            path: "/api/cart/items/\(cartItemId)",
            method: "PUT",
            body: body,
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(CartResponse.self, from: data)
    }

    func removeItem(cartItemId: Int) async throws -> CartResponse {
        let data = try await APIClient.shared.request(
            path: "/api/cart/items/\(cartItemId)",
            method: "DELETE",
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(CartResponse.self, from: data)
    }
}
