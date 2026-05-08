import SwiftUI
import Combine

@MainActor
final class WishlistViewModel: ObservableObject {
    @Published var products: [Product] = []
    @Published var isLoading = false

    func load(ids: [Int]) async {
        guard !ids.isEmpty else {
            products = []
            return
        }
        isLoading = true
        defer { isLoading = false }
        var loaded: [Product] = []
        for id in ids {
            do {
                let product = try await ProductService.shared.getProductAsync(by: String(id))
                loaded.append(product)
            } catch {
                continue
            }
        }
        products = loaded
    }
}

struct WishlistView: View {
    @EnvironmentObject private var wishlistStore: WishlistStore
    @StateObject private var viewModel = WishlistViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if wishlistStore.productIds.isEmpty {
                    ContentUnavailableView("Chưa có sản phẩm yêu thích", systemImage: "heart")
                } else if viewModel.isLoading {
                    ProgressView()
                } else {
                    List(viewModel.products) { product in
                        NavigationLink {
                            ProductDetailView(product: product)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.name ?? "Sản phẩm")
                                Text(product.priceRange).font(.caption).foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Yêu thích")
            .onAppear {
                Task { await viewModel.load(ids: Array(wishlistStore.productIds)) }
            }
            .onChange(of: wishlistStore.productIds) { _, newValue in
                Task { await viewModel.load(ids: Array(newValue)) }
            }
        }
    }
}
