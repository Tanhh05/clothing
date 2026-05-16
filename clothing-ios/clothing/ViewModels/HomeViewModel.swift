import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var categories: [Category] = []
    @Published var selectedCategoryId: Int? = nil
    @Published var products: [Product] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var hasNextPage = true
    private var currentPage = 0
    private let pageSize = 10

    func loadInitial() async {
        isLoading = true
        defer { isLoading = false }
        do {
            categories = try await CategoryService.shared.getCategories()
            currentPage = 0
            hasNextPage = true
            products = []
            try await loadProducts(reset: true)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func loadMore() async {
        guard hasNextPage else { return }
        do {
            try await loadProducts(reset: false)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func selectCategory(_ categoryId: Int?) async {
        selectedCategoryId = categoryId
        currentPage = 0
        hasNextPage = true
        products = []
        do {
            try await loadProducts(reset: true)
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func loadProducts(reset: Bool) async throws {
        let page = try await ProductService.shared.getProductsAsync(
            page: currentPage,
            size: pageSize,
            sortBy: "createdAt",
            direction: "desc",
            category: selectedCategoryId
        )
        let items = page.content ?? []
        if reset {
            products = items
        } else {
            products.append(contentsOf: items)
        }
        hasNextPage = !(page.last ?? true)
        currentPage += 1
    }
}
