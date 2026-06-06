import SwiftUI

struct ProductDetailView: View {
    let product: Product
    @EnvironmentObject private var cartStore: CartStore
    @EnvironmentObject private var wishlistStore: WishlistStore
    @State private var selectedVariant: ProductVariant?
    @State private var selectedSize: String = ""
    @State private var selectedColor: String = ""
    @State private var showAlert = false
    @State private var alertText = ""

    private var displayVariant: ProductVariant? {
        if let selectedVariant { return selectedVariant }
        if !selectedSize.isEmpty || !selectedColor.isEmpty {
            return product.variants?.first(where: { variant in
                let sizeOK = selectedSize.isEmpty || (variant.size ?? "") == selectedSize
                let colorOK = selectedColor.isEmpty || (variant.color ?? "") == selectedColor
                return sizeOK && colorOK
            })
        }
        return product.variants?.first
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                TabView {
                    ForEach(product.images ?? [], id: \.id) { image in
                        AsyncImage(url: URL(string: image.url ?? "")) { phase in
                            switch phase {
                            case .success(let img): img.resizable().scaledToFill()
                            default: Rectangle().fill(Color.gray.opacity(0.2))
                            }
                        }
                        .frame(height: 350)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
                .frame(height: 350)
                .tabViewStyle(.page(indexDisplayMode: .never))

                Text((product.categoryName ?? "NEW COLLECTION").uppercased())
                    .font(.caption)
                    .foregroundColor(.gray)
                    .tracking(2)

                Text(product.name ?? "Sản phẩm")
                    .font(.system(size: 46, weight: .regular, design: .serif))

                Text(displayVariant?.formattedPrice ?? product.priceRange)
                    .font(.system(size: 38, weight: .medium, design: .serif))
                    .foregroundColor(Color(red: 0.42, green: 0.26, blue: 0.05))

                if !product.availableColors.isEmpty {
                    Text("MÀU SẮC")
                        .font(.headline)
                        .padding(.top, 4)
                    HStack(spacing: 12) {
                        ForEach(product.availableColors, id: \.self) { color in
                            Button {
                                selectedColor = color
                                selectedVariant = nil
                            } label: {
                                Text(color)
                                    .font(.subheadline)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(selectedColor == color ? Color.black : Color.white)
                                    .foregroundColor(selectedColor == color ? .white : .black)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(Color.black.opacity(0.2), lineWidth: 1)
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                        }
                    }
                }

                if !product.availableSizes.isEmpty {
                    HStack {
                        Text("KÍCH THƯỚC").font(.headline)
                        Spacer()
                        Text("Hướng dẫn chọn size").font(.caption).underline()
                    }
                    HStack(spacing: 10) {
                        ForEach(product.availableSizes, id: \.self) { size in
                            Button {
                                selectedSize = size
                                selectedVariant = nil
                            } label: {
                                Text(size)
                                    .font(.headline)
                                    .frame(width: 54, height: 44)
                                    .background(selectedSize == size ? Color.black : Color.white)
                                    .foregroundColor(selectedSize == size ? .white : .black)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(Color.black.opacity(0.2), lineWidth: 1)
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                        }
                    }
                }

                Text("Mô tả chi tiết")
                    .font(.system(size: 44, weight: .regular, design: .serif))
                    .padding(.top, 6)
                Text(product.description ?? "")
                    .font(.body)
                    .foregroundColor(.primary.opacity(0.9))

                VStack(alignment: .leading, spacing: 6) {
                    Text("• Chất liệu cao cấp")
                    Text("• Thiết kế tối giản hiện đại")
                    Text("• Hoàn thiện tỉ mỉ")
                }
                .font(.body)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 88)
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
                Image(systemName: "square.and.arrow.up")
            }
        }
        .safeAreaInset(edge: .bottom) {
            HStack(spacing: 10) {
                Button {
                    wishlistStore.toggle(product.id)
                } label: {
                    Image(systemName: wishlistStore.contains(product.id) ? "heart.fill" : "heart")
                        .frame(width: 52, height: 52)
                        .background(Color.white)
                        .foregroundColor(.black)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.black.opacity(0.2), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                Button {
                    guard let variant = displayVariant else {
                        alertText = "Vui lòng chọn biến thể phù hợp."
                        showAlert = true
                        return
                    }
                    if let stock = variant.stock, stock <= 0 {
                        alertText = "Sản phẩm đã hết hàng."
                        showAlert = true
                        return
                    }
                    cartStore.add(product: product, variant: variant)
                    alertText = "Đã thêm vào giỏ hàng."
                    showAlert = true
                } label: {
                    HStack {
                        Image(systemName: "bag")
                        Text("THÊM VÀO GIỎ HÀNG")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(red: 0.52, green: 0.39, blue: 0.10))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color(white: 0.96))
        }
        .alert("Thông báo", isPresented: $showAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertText)
        }
    }
}
