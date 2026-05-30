import Foundation

// MARK: - Request Models

struct LoginRequest: Codable {
    var usernameOrEmail: String
    var password: String
}

struct RegisterRequest: Codable {
    var fullName: String
    var username: String
    var email: String
    var password: String
}

struct RefreshTokenRequest: Codable {
    var refreshToken: String
}

struct LogoutRequest: Codable {
    var refreshToken: String
}

// MARK: - Response Models

struct AuthResponse: Codable {
    let accessToken: String?
    let refreshToken: String?
    let tokenType: String?
    let expiresIn: Int?
    let refreshExpiresIn: Int?
    let userId: Int?
    let username: String?
    let email: String?
    let fullName: String?
    let phone: String?
    let roles: [String]?
}

// MARK: - Error Response (khi server trả lỗi)

struct ErrorResponse: Codable {
    let message: String?
    let error: String?
    let status: Int?
}

struct ApiErrorEnvelope: Codable {
    let data: ErrorResponse?
}
