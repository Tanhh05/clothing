import Foundation

// MARK: - Product Image

struct ProductImage: Codable, Identifiable {
    let id: Int?
    let url: String?
    let isMain: Bool?
}

// MARK: - Product Variant

struct ProductVariant: Codable, Identifiable {
    let id: Int?
    let sku: String?
    let price: Int?
    let stock: Int?
    let weight: Double?
    let status: String?
    let size: String?
    let color: String?
    
    /// Giá hiển thị dạng VND
    var formattedPrice: String {
        guard let price = price else { return "Liên hệ" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        let formatted = formatter.string(from: NSNumber(value: price)) ?? "\(price)"
        return "\(formatted)₫"
    }
}

// MARK: - Product

struct Product: Codable, Identifiable {
    let id: Int?
    let name: String?
    let slug: String?
    let description: String?
    let brand: String?
    let categoryId: Int?
    let categoryName: String?
    let status: String?
    let createdAt: String?
    let ratingAvg: Double?
    let reviewCount: Int?
    let variants: [ProductVariant]?
    let images: [ProductImage]?
    
    /// Lấy ảnh chính (isMain = true), nếu không có thì lấy ảnh đầu tiên
    var mainImageURL: String? {
        if let mainImage = images?.first(where: { $0.isMain == true }) {
            return mainImage.url
        }
        return images?.first?.url
    }
    
    /// Giá thấp nhất từ các variant
    var minPrice: Int? {
        variants?.compactMap { $0.price }.min()
    }
    
    /// Giá cao nhất từ các variant
    var maxPrice: Int? {
        variants?.compactMap { $0.price }.max()
    }
    
    /// Hiển thị khoảng giá
    var priceRange: String {
        guard let min = minPrice else { return "Liên hệ" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        
        let minStr = formatter.string(from: NSNumber(value: min)) ?? "\(min)"
        
        if let max = maxPrice, max != min {
            let maxStr = formatter.string(from: NSNumber(value: max)) ?? "\(max)"
            return "\(minStr)₫ - \(maxStr)₫"
        }
        return "\(minStr)₫"
    }
    
    /// Tổng tồn kho
    var totalStock: Int {
        variants?.compactMap { $0.stock }.reduce(0, +) ?? 0
    }
    
    /// Các size có sẵn
    var availableSizes: [String] {
        let sizes = variants?.compactMap { $0.size } ?? []
        return Array(Set(sizes)).sorted()
    }
    
    /// Các màu có sẵn
    var availableColors: [String] {
        let colors = variants?.compactMap { $0.color } ?? []
        return Array(Set(colors)).sorted()
    }
}

// MARK: - Page Response (phân trang)

struct PageResponse<T: Codable>: Codable {
    let content: [T]?
    let page: Int?
    let size: Int?
    let totalElements: Int?
    let totalPages: Int?
    let first: Bool?
    let last: Bool?
}
