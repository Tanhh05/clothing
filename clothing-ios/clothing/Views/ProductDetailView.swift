import SwiftUI

struct ProductDetailView: View {
    let product: Product
    @EnvironmentObject private var cartStore: CartStore
    @EnvironmentObject private var wishlistStore: WishlistStore
    @State private var selectedVariant: ProductVariant?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                AsyncImage(url: URL(string: product.mainImageURL ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        Rectangle().fill(Color.gray.opacity(0.2))
                    }
                }
                .frame(height: 280)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                Text(product.name ?? "Sản phẩm")
                    .font(.title3.bold())
                Text(selectedVariant?.formattedPrice ?? product.priceRange)
                    .font(.headline)
                    .foregroundColor(.blue)

                if let variants = product.variants, !variants.isEmpty {
                    Text("Biến thể").font(.subheadline.bold())
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(variants) { variant in
                                Button {
                                    selectedVariant = variant
                                } label: {
                                    Text("\(variant.color ?? "") \(variant.size ?? "")")
                                        .font(.caption)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 8)
                                        .background((selectedVariant?.id == variant.id) ? Color.black : Color.gray.opacity(0.15))
                                        .foregroundColor((selectedVariant?.id == variant.id) ? .white : .black)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }

                if let description = product.description, !description.isEmpty {
                    Text(description).font(.body).foregroundColor(.secondary)
                }

                HStack(spacing: 12) {
                    Button {
                        wishlistStore.toggle(product.id)
                    } label: {
                        Image(systemName: wishlistStore.contains(product.id) ? "heart.fill" : "heart")
                            .frame(width: 44, height: 44)
                            .background(Color.gray.opacity(0.15))
                            .clipShape(Circle())
                    }

                    Button {
                        cartStore.add(product: product, variant: selectedVariant ?? product.variants?.first)
                    } label: {
                        Text("Thêm vào giỏ")
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.black)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
            .padding(16)
        }
        .navigationTitle("Chi tiết")
        .navigationBarTitleDisplayMode(.inline)
    }
}

