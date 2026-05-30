import Foundation

struct UpdateProfileRequest: Codable {
    let username: String?
    let fullName: String?
    let email: String?
    let phone: String?
}

final class UserService {
    static let shared = UserService()
    private init() {}

    func updateProfile(_ req: UpdateProfileRequest) async throws {
        let body = try JSONEncoder().encode(req)
        _ = try await APIClient.shared.request(
            path: "/api/user/me",
            method: "PUT",
            body: body,
            requiresAuth: true
        )
    }

    func getAddresses() async throws -> [UserAddress] {
        let data = try await APIClient.shared.request(path: "/api/user/addresses", requiresAuth: true)
        return try APIClient.shared.decodeResponse([UserAddress].self, from: data)
    }

    func createAddress(_ req: UserAddressUpsertRequest) async throws -> UserAddress {
        let body = try JSONEncoder().encode(req)
        let data = try await APIClient.shared.request(
            path: "/api/user/addresses",
            method: "POST",
            body: body,
            requiresAuth: true
        )
        return try APIClient.shared.decodeResponse(UserAddress.self, from: data)
    }
}
