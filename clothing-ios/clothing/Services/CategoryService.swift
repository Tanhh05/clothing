import Foundation

final class CategoryService {
    static let shared = CategoryService()
    private init() {}

    func getCategories(page: Int = 0, size: Int = 20) async throws -> [Category] {
        let data = try await APIClient.shared.request(
            path: "/api/categories",
            queryItems: [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "size", value: String(size)),
                URLQueryItem(name: "sortBy", value: "id"),
                URLQueryItem(name: "direction", value: "asc")
            ]
        )
        let response = try APIClient.shared.decodeResponse(PageResponse<Category>.self, from: data)
        return response.content ?? []
    }
}
