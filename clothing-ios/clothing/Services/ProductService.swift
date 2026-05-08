import Foundation
import Combine

class ProductService {
    static let shared = ProductService()
    
    private let baseURL = AppConfig.backendBaseURL + "/api/products"
    
    private init() {}
    
    // MARK: - Lấy danh sách sản phẩm (có phân trang)
    
    func getProducts(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "id",
        direction: String = "desc",
        category: Int? = nil,
        query: String? = nil
    ) -> AnyPublisher<PageResponse<Product>, Error> {
        var components = URLComponents(string: baseURL)!
        var queryItems = [
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "size", value: "\(size)"),
            URLQueryItem(name: "sortBy", value: sortBy),
            URLQueryItem(name: "direction", value: direction)
        ]
        
        if let category = category {
            queryItems.append(URLQueryItem(name: "category", value: "\(category)"))
        }
        if let query = query, !query.isEmpty {
            queryItems.append(URLQueryItem(name: "q", value: query))
        }
        
        components.queryItems = queryItems
        
        guard let url = components.url else {
            return Fail(error: AuthError.invalidURL).eraseToAnyPublisher()
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 30
        
        // Thêm token nếu đã đăng nhập
        if let token = TokenManager.shared.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return URLSession.shared.dataTaskPublisher(for: request)
            .tryMap { data, response -> Data in
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw AuthError.unknown
                }
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw AuthError.serverError("Lỗi server: \(httpResponse.statusCode)")
                }
                return data
            }
            .decode(type: PageResponse<Product>.self, decoder: JSONDecoder())
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
    
    // MARK: - Lấy chi tiết sản phẩm
    
    func getProduct(by slugOrId: String) -> AnyPublisher<Product, Error> {
        guard let url = URL(string: "\(baseURL)/\(slugOrId)") else {
            return Fail(error: AuthError.invalidURL).eraseToAnyPublisher()
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 30
        
        if let token = TokenManager.shared.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return URLSession.shared.dataTaskPublisher(for: request)
            .tryMap { data, response -> Data in
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw AuthError.unknown
                }
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw AuthError.serverError("Lỗi server: \(httpResponse.statusCode)")
                }
                return data
            }
            .decode(type: Product.self, decoder: JSONDecoder())
            .mapError { error -> Error in
                if let authError = error as? AuthError {
                    return authError
                }
                if error is URLError {
                    return AuthError.networkError
                }
                return AuthError.unknown
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }

    func getProductAsync(by slugOrId: String) async throws -> Product {
        let data = try await APIClient.shared.request(path: "/api/products/\(slugOrId)")
        do {
            return try JSONDecoder().decode(Product.self, from: data)
        } catch {
            throw APIError.decoding
        }
    }

    func getProductsAsync(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "id",
        direction: String = "desc",
        category: Int? = nil,
        query: String? = nil
    ) async throws -> PageResponse<Product> {
        var items = [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "size", value: String(size)),
            URLQueryItem(name: "sortBy", value: sortBy),
            URLQueryItem(name: "direction", value: direction)
        ]
        if let category {
            items.append(URLQueryItem(name: "category", value: String(category)))
        }
        if let query, !query.isEmpty {
            items.append(URLQueryItem(name: "q", value: query))
        }

        let data = try await APIClient.shared.request(path: "/api/products", queryItems: items)
        do {
            return try JSONDecoder().decode(PageResponse<Product>.self, from: data)
        } catch {
            throw APIError.decoding
        }
    }
}
