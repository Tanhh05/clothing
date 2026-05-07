import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var cartStore: CartStore
    @EnvironmentObject private var wishlistStore: WishlistStore

    private let bestSellingColumns = [GridItem(.flexible()), GridItem(.flexible())]
    private let featuredColumns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 28) {
                    heroSection
                    bestSellingSection
                    featuredSection
                }
                .padding(.bottom, 12)
            }
            .background(Color(white: 0.98))
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 86)
            }
            .navigationTitle("Twenty")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                if viewModel.featuredProducts.isEmpty && viewModel.bestSellingProducts.isEmpty {
                    await viewModel.loadInitial()
                }
            }
            .refreshable {
                await viewModel.loadInitial()
            }
        }
    }

    private var heroSection: some View {
        VStack(spacing: 12) {
            LinearGradient(colors: [.black.opacity(0.78), .gray.opacity(0.45)],
                           startPoint: .topLeading,
                           endPoint: .bottomTrailing)
                .frame(height: 220)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(alignment: .bottomLeading) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("New Arrivals")
                        .font(.title3.bold())
                        .foregroundColor(.white)
                    Text("Mẫu mới cập nhật từ storefront")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.9))
                }
                .padding(14)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 10)
    }

    private var bestSellingSection: some View {
        VStack(spacing: 14) {
            sectionHeader(title: "Best Selling", subtitle: "Top sản phẩm nổi bật")

            if viewModel.isLoadingBestSelling && viewModel.bestSellingProducts.isEmpty {
                ProgressView().frame(maxWidth: .infinity)
            } else if let err = viewModel.bestSellingError, viewModel.bestSellingProducts.isEmpty {
                Text(err).foregroundColor(.red).padding(.horizontal, 16)
            } else {
                LazyVGrid(columns: bestSellingColumns, spacing: 12) {
                    ForEach(viewModel.bestSellingProducts) { product in
                        NavigationLink {
                            ProductDetailView(product: product)
                        } label: {
                            ProductCard(product: product, isFavorite: wishlistStore.contains(product.id))
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button("Thêm giỏ hàng") {
                                cartStore.add(product: product, variant: product.variants?.first)
                            }
                            Button(wishlistStore.contains(product.id) ? "Bỏ yêu thích" : "Yêu thích") {
                                wishlistStore.toggle(product.id)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var featuredSection: some View {
        VStack(spacing: 14) {
            sectionHeader(title: "Featured Products", subtitle: "Danh sách đồng bộ storefront")

            if viewModel.isLoadingFeatured && viewModel.featuredProducts.isEmpty {
                ProgressView().frame(maxWidth: .infinity)
            } else if let err = viewModel.featuredError, viewModel.featuredProducts.isEmpty {
                Text(err).foregroundColor(.red).padding(.horizontal, 16)
            } else {
                LazyVGrid(columns: featuredColumns, spacing: 12) {
                    ForEach(viewModel.featuredProducts) { product in
                        NavigationLink {
                            ProductDetailView(product: product)
                        } label: {
                            ProductCard(product: product, isFavorite: wishlistStore.contains(product.id))
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button("Thêm giỏ hàng") {
                                cartStore.add(product: product, variant: product.variants?.first)
                            }
                            Button(wishlistStore.contains(product.id) ? "Bỏ yêu thích" : "Yêu thích") {
                                wishlistStore.toggle(product.id)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)

                Button {
                    Task { await viewModel.loadMoreFeatured() }
                } label: {
                    Text(viewModel.isLoadingFeatured ? "Đang tải..." : "See More")
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Color.black)
                        .foregroundColor(.white)
                        .clipShape(Capsule())
                }
                .disabled(viewModel.isLoadingFeatured)
            }
        }
    }

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(spacing: 6) {
            Text(title)
                .font(.title3.weight(.semibold))
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct ProductCard: View {
    let product: Product
    let isFavorite: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: product.mainImageURL ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        Rectangle().fill(Color.gray.opacity(0.15))
                    }
                }
                .frame(height: 148)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                if isFavorite {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.pink)
                        .padding(8)
                }
            }

            Text(product.name ?? "Sản phẩm")
                .font(.subheadline.weight(.semibold))
                .lineLimit(2)
                .frame(height: 38, alignment: .topLeading)

            Text(product.priceRange)
                .font(.subheadline.bold())
                .foregroundColor(.blue)

            Spacer(minLength: 0)
        }
        .padding(10)
        .frame(maxWidth: .infinity, minHeight: 252, maxHeight: 252, alignment: .topLeading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 4, y: 1)
    }
}
