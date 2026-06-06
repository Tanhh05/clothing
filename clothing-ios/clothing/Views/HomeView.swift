import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var cartStore: CartStore
    @EnvironmentObject private var wishlistStore: WishlistStore
    private let columns = [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    heroSection
                    categorySection
                    productsSection
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
            .background(Color(white: 0.96))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image(systemName: "line.3.horizontal")
                }
                ToolbarItem(placement: .principal) {
                    Text("Clothing")
                        .font(.system(size: 34, weight: .black, design: .serif))
                        .tracking(1)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Image(systemName: "bell")
                }
            }
            .task {
                if viewModel.products.isEmpty {
                    await viewModel.loadInitial()
                }
            }
            .refreshable {
                await viewModel.loadInitial()
            }
        }
    }

    private var heroSection: some View {
        let heroProduct = viewModel.products.first
        return ZStack(alignment: .bottomLeading) {
            AsyncImage(url: URL(string: heroProduct?.mainImageURL ?? "")) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    Rectangle().fill(Color.black.opacity(0.4))
                }
            }
            .frame(height: 370)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay {
                LinearGradient(colors: [.clear, .black.opacity(0.5)], startPoint: .center, endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Bộ Sưu Tập Mới")
                    .font(.title3.weight(.medium))
                    .foregroundColor(.white)
                Text("Phong cách tối giản, hiện đại cho ngày mới.")
                    .font(.body)
                    .foregroundColor(.white.opacity(0.95))
                Button("Mua Ngay") {}
                    .font(.title3.weight(.medium))
                    .padding(.horizontal, 32)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .foregroundColor(.black)
                    .clipShape(Capsule())
            }
            .padding(20)
        }
    }

    private var categorySection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                chip(title: "Tất cả", isSelected: viewModel.selectedCategoryId == nil) {
                    Task { await viewModel.selectCategory(nil) }
                }
                ForEach(viewModel.categories, id: \.id) { category in
                    chip(
                        title: category.name,
                        isSelected: viewModel.selectedCategoryId == category.id
                    ) {
                        Task { await viewModel.selectCategory(category.id) }
                    }
                }
            }
        }
    }

    private var productsSection: some View {
        VStack(spacing: 14) {
            HStack {
                Text("Nổi bật nhất")
                    .font(.system(size: 42, weight: .regular, design: .serif))
                Spacer()
                NavigationLink("Xem tất cả", destination: SearchView())
                    .foregroundColor(.gray)
            }

            if viewModel.isLoading && viewModel.products.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 120)
            } else if let error = viewModel.error, viewModel.products.isEmpty {
                Text(error).foregroundColor(.red)
            } else {
                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(viewModel.products) { product in
                        NavigationLink {
                            ProductDetailView(product: product)
                        } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                ZStack(alignment: .bottomTrailing) {
                                    AsyncImage(url: URL(string: product.mainImageURL ?? "")) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image.resizable().scaledToFill()
                                        default:
                                            Rectangle().fill(Color.gray.opacity(0.2))
                                        }
                                    }
                                    .frame(height: 190)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))

                                    Button {
                                        cartStore.add(product: product, variant: product.variants?.first)
                                    } label: {
                                        Image(systemName: "bag")
                                            .foregroundColor(.black)
                                            .padding(10)
                                            .background(Color.white.opacity(0.9))
                                            .clipShape(Circle())
                                    }
                                    .padding(8)
                                }
                                Text((product.categoryName ?? "Sản phẩm").uppercased())
                                    .font(.caption)
                                    .foregroundColor(.gray)
                                Text(product.name ?? "Sản phẩm")
                                    .font(.title3.weight(.medium))
                                    .foregroundColor(.black)
                                    .lineLimit(2)
                                Text(product.priceRange)
                                    .font(.title3.weight(.medium))
                                    .foregroundColor(Color(red: 0.42, green: 0.26, blue: 0.05))
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                if viewModel.hasNextPage {
                    Button("Xem thêm") {
                        Task { await viewModel.loadMore() }
                    }
                    .font(.headline)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity)
                    .background(Color.black)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }

    private func chip(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .padding(.horizontal, 22)
                .padding(.vertical, 9)
                .background(isSelected ? Color.black : Color(white: 0.92))
                .foregroundColor(isSelected ? .white : .black)
                .clipShape(Capsule())
        }
    }
}
