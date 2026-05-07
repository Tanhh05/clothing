import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case unauthorized
    case server(String)
    case network(String)
    case decoding

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "URL không hợp lệ."
        case .unauthorized: return "Phiên đăng nhập đã hết hạn."
        case .server(let message): return message
        case .network(let message): return message
        case .decoding: return "Không thể đọc dữ liệu từ server."
        }
    }
}

final class APIClient {
    static let shared = APIClient()
    private init() {}

    func request(
        path: String,
        method: String = "GET",
        queryItems: [URLQueryItem] = [],
        body: Data? = nil,
        requiresAuth: Bool = false
    ) async throws -> Data {
        guard var components = URLComponents(string: AppConfig.backendBaseURL + path) else {
            throw APIError.invalidURL
        }
        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 30
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if requiresAuth, let token = TokenManager.shared.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = body

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw APIError.network("Không nhận được phản hồi hợp lệ.")
            }

            switch http.statusCode {
            case 200...299:
                return data
            case 401:
                throw APIError.unauthorized
            default:
                if let message = String(data: data, encoding: .utf8), !message.isEmpty {
                    throw APIError.server("Lỗi \(http.statusCode): \(message)")
                }
                throw APIError.server("Lỗi server: \(http.statusCode)")
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.network(error.localizedDescription)
        }
    }
}

