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
        return try JSONDecoder().decode([UserAddress].self, from: data)
    }
}

