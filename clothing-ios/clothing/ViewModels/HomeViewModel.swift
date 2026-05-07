import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var featuredProducts: [Product] = []
    @Published var bestSellingProducts: [Product] = []
    @Published var isLoadingFeatured = false
    @Published var isLoadingBestSelling = false
    @Published var featuredError: String?
    @Published var bestSellingError: String?

    private var featuredPage = 0
    private let featuredSize = 10

    func loadInitial() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.loadFeatured(reset: true) }
            group.addTask { await self.loadBestSelling() }
        }
    }

    func loadMoreFeatured() async {
        await loadFeatured(reset: false)
    }

    private func loadFeatured(reset: Bool) async {
        if isLoadingFeatured { return }
        isLoadingFeatured = true
        featuredError = nil
        defer { isLoadingFeatured = false }

        if reset {
            featuredPage = 0
            featuredProducts = []
        }

        do {
            let page = try await ProductService.shared.getProductsAsync(
                page: featuredPage,
                size: featuredSize,
                sortBy: "createdAt",
                direction: "desc"
            )
            let items = page.content ?? []
            if reset {
                featuredProducts = items
            } else {
                featuredProducts.append(contentsOf: items)
            }
            featuredPage += 1
        } catch {
            featuredError = error.localizedDescription
        }
    }

    private func loadBestSelling() async {
        if isLoadingBestSelling { return }
        isLoadingBestSelling = true
        bestSellingError = nil
        defer { isLoadingBestSelling = false }

        do {
            let page = try await ProductService.shared.getProductsAsync(
                page: 0,
                size: 4,
                sortBy: "createdAt",
                direction: "desc"
            )
            bestSellingProducts = page.content ?? []
        } catch {
            bestSellingError = error.localizedDescription
        }
    }
}

