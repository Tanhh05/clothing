import Foundation

final class WishlistService {
    static let shared = WishlistService()
    private init() {}

    func getMyWishlist() async throws -> WishlistResponse {
        let data = try await APIClient.shared.request(path: "/api/wishlist", requiresAuth: true)
        return try APIClient.shared.decodeResponse(WishlistResponse.self, from: data)
    }

    func addItem(productId: Int) async throws -> WishlistResponse {
        let body = try JSONEncoder().encode(ToggleWishlistItemRequest(productId: productId))
        let data = try await APIClient.shared.request(
            path: "/api/wishlist/items",
            method: "POST",
            body: body,
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(WishlistResponse.self, from: data)
    }

    func removeItem(productId: Int) async throws -> WishlistResponse {
        let data = try await APIClient.shared.request(
            path: "/api/wishlist/items/\(productId)",
            method: "DELETE",
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(WishlistResponse.self, from: data)
    }
}
