import SwiftUI

struct SearchView: View {
    @StateObject private var viewModel = ProductViewModel()

    var body: some View {
        NavigationStack {
            List {
                ForEach(viewModel.products) { product in
                    NavigationLink {
                        ProductDetailView(product: product)
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(product.name ?? "Sản phẩm").font(.body.weight(.semibold))
                            Text(product.priceRange).font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
                if viewModel.isLoading {
                    HStack { Spacer(); ProgressView(); Spacer() }
                }
            }
            .searchable(text: $viewModel.searchText, prompt: "Tìm sản phẩm")
            .navigationTitle("Tìm kiếm")
            .onAppear {
                if viewModel.products.isEmpty {
                    viewModel.loadProducts()
                }
            }
        }
    }
}

