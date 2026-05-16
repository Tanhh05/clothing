import Foundation
import Combine

extension Notification.Name {
    static let authStateDidChange = Notification.Name("authStateDidChange")
}

// MARK: - Auth Error

enum AuthError: Error, LocalizedError {
    case invalidURL
    case serverError(String)
    case decodingError
    case unauthorized
    case networkError
    case unknown
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "URL không hợp lệ."
        case .serverError(let message):
            return message
        case .decodingError:
            return "Lỗi phân tích dữ liệu phản hồi."
        case .unauthorized:
            return "Sai tên đăng nhập hoặc mật khẩu."
        case .networkError:
            return "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng."
        case .unknown:
            return "Đã xảy ra lỗi không xác định."
        }
    }
}

// MARK: - Token Manager

class TokenManager {
    static let shared = TokenManager()
    
    private let accessTokenKey = "accessToken"
    private let refreshTokenKey = "refreshToken"
    private let userIdKey = "userId"
    private let usernameKey = "username"
    private let emailKey = "email"
    private let fullNameKey = "fullName"
    private let rolesKey = "roles"
    
    private init() {}
    
    func saveAuthResponse(_ response: AuthResponse) {
        if let accessToken = response.accessToken {
            UserDefaults.standard.set(accessToken, forKey: accessTokenKey)
        }
        if let refreshToken = response.refreshToken {
            UserDefaults.standard.set(refreshToken, forKey: refreshTokenKey)
        }
        if let userId = response.userId {
            UserDefaults.standard.set(userId, forKey: userIdKey)
        }
        if let username = response.username {
            UserDefaults.standard.set(username, forKey: usernameKey)
        }
        if let email = response.email {
            UserDefaults.standard.set(email, forKey: emailKey)
        }
        if let fullName = response.fullName {
            UserDefaults.standard.set(fullName, forKey: fullNameKey)
        }
        if let roles = response.roles {
            UserDefaults.standard.set(roles, forKey: rolesKey)
        }
        NotificationCenter.default.post(name: .authStateDidChange, object: nil)
    }
    
    var accessToken: String? {
        UserDefaults.standard.string(forKey: accessTokenKey)
    }
    
    var refreshToken: String? {
        UserDefaults.standard.string(forKey: refreshTokenKey)
    }
    
    var userId: Int? {
        let val = UserDefaults.standard.integer(forKey: userIdKey)
        return val == 0 ? nil : val
    }
    
    var username: String? {
        UserDefaults.standard.string(forKey: usernameKey)
    }
    
    var email: String? {
        UserDefaults.standard.string(forKey: emailKey)
    }
    
    var fullName: String? {
        UserDefaults.standard.string(forKey: fullNameKey)
    }
    
    var roles: [String]? {
        UserDefaults.standard.stringArray(forKey: rolesKey)
    }
    
    var isLoggedIn: Bool {
        accessToken != nil
    }
    
    func clearAll() {
        UserDefaults.standard.removeObject(forKey: accessTokenKey)
        UserDefaults.standard.removeObject(forKey: refreshTokenKey)
        UserDefaults.standard.removeObject(forKey: userIdKey)
        UserDefaults.standard.removeObject(forKey: usernameKey)
        UserDefaults.standard.removeObject(forKey: emailKey)
        UserDefaults.standard.removeObject(forKey: fullNameKey)
        UserDefaults.standard.removeObject(forKey: rolesKey)
        NotificationCenter.default.post(name: .authStateDidChange, object: nil)
    }
}

// MARK: - Auth Service

class AuthService {
    static let shared = AuthService()
    
    private let baseURL = AppConfig.backendBaseURL + "/api/auth"
    
    private init() {}
    
    // MARK: - Đăng nhập
    
    func login(request: LoginRequest) -> AnyPublisher<AuthResponse, Error> {
        return performRequest(
            endpoint: "/login",
            body: request
        )
    }
    
    // MARK: - Đăng ký
    
    func register(request: RegisterRequest) -> AnyPublisher<AuthResponse, Error> {
        return performRequest(
            endpoint: "/register",
            body: request
        )
    }
    
    // MARK: - Refresh Token
    
    func refreshToken() -> AnyPublisher<AuthResponse, Error> {
        guard let refreshToken = TokenManager.shared.refreshToken else {
            return Fail(error: AuthError.unauthorized).eraseToAnyPublisher()
        }
        
        let request = RefreshTokenRequest(refreshToken: refreshToken)
        return performRequest(
            endpoint: "/refresh",
            body: request
        )
    }
    
    // MARK: - Đăng xuất
    
    func logout() -> AnyPublisher<Void, Error> {
        guard let refreshToken = TokenManager.shared.refreshToken else {
            TokenManager.shared.clearAll()
            return Just(()).setFailureType(to: Error.self).eraseToAnyPublisher()
        }
        
        guard let url = URL(string: "\(baseURL)/logout") else {
            return Fail(error: AuthError.invalidURL).eraseToAnyPublisher()
        }
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let accessToken = TokenManager.shared.accessToken {
            urlRequest.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }
        
        let body = LogoutRequest(refreshToken: refreshToken)
        urlRequest.httpBody = try? JSONEncoder().encode(body)
        
        return URLSession.shared.dataTaskPublisher(for: urlRequest)
            .tryMap { _, response -> Void in
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw AuthError.unknown
                }
                // 204 No Content = thành công
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw AuthError.serverError("Lỗi đăng xuất: \(httpResponse.statusCode)")
                }
                TokenManager.shared.clearAll()
                return ()
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
    
    // MARK: - Generic Request Helper
    
    private func performRequest<T: Encodable>(
        endpoint: String,
        body: T
    ) -> AnyPublisher<AuthResponse, Error> {
        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            return Fail(error: AuthError.invalidURL).eraseToAnyPublisher()
        }
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.timeoutInterval = 30
        
        do {
            let encodedData = try JSONEncoder().encode(body)
            urlRequest.httpBody = encodedData
        } catch {
            return Fail(error: error).eraseToAnyPublisher()
        }
        
        return URLSession.shared.dataTaskPublisher(for: urlRequest)
            .tryMap { data, response -> AuthResponse in
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw AuthError.unknown
                }
                
                // Xử lý các mã lỗi HTTP
                switch httpResponse.statusCode {
                case 200...299:
                    let decoder = JSONDecoder()
                    if let wrapped = try? decoder.decode(APIEnvelope<AuthResponse>.self, from: data),
                       let unwrapped = wrapped.data,
                       let token = unwrapped.accessToken,
                       !token.isEmpty {
                        return unwrapped
                    }
                    if let direct = try? decoder.decode(AuthResponse.self, from: data),
                       let token = direct.accessToken,
                       !token.isEmpty {
                        return direct
                    }
                    throw AuthError.decodingError
                case 401:
                    throw AuthError.unauthorized
                default:
                    // Cố gắng đọc message lỗi từ server
                    if let wrappedError = try? JSONDecoder().decode(ApiErrorEnvelope.self, from: data),
                       let innerError = wrappedError.data {
                        let message = innerError.message ?? innerError.error ?? "Lỗi server"
                        throw AuthError.serverError(message)
                    }
                    if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                        let message = errorResponse.message ?? errorResponse.error ?? "Lỗi server"
                        throw AuthError.serverError(message)
                    }
                    throw AuthError.serverError("Lỗi server: \(httpResponse.statusCode)")
                }
            }
            .mapError { error -> Error in
                if let authError = error as? AuthError {
                    return authError
                }
                if error is URLError {
                    return AuthError.networkError
                }
                if error is DecodingError {
                    return AuthError.decodingError
                }
                return AuthError.unknown
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
}
