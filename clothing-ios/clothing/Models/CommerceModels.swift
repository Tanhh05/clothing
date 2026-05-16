import Foundation

struct Category: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
}

struct UserAddress: Codable, Identifiable {
    let id: Int
    let recipientName: String
    let phone: String
    let province: String
    let district: String
    let ward: String
    let addressLine: String
    let isDefault: Bool
}

struct UserAddressUpsertRequest: Codable {
    let recipientName: String
    let phone: String
    let province: String
    let district: String
    let ward: String
    let addressLine: String
    let isDefault: Bool
}

struct OrderItem: Codable, Identifiable {
    let id: Int?
    let productId: Int?
    let productName: String?
    let quantity: Int?
    let price: Double?
    let lineTotal: Double?
}

struct OrderSummary: Codable, Identifiable {
    let id: Int
    let status: String?
    let paymentMethod: String?
    let totalPrice: Double?
    let createdAt: String?
    let items: [OrderItem]?
    let paymentUrl: String?
    let subTotal: Double?
    let shippingFee: Double?
    let discountAmount: Double?
    let appliedVoucherCode: String?
}

struct CreateOrderRequest: Codable {
    let paymentMethod: String
    let address: String
    let recipientName: String?
    let phone: String?
    let province: String?
    let district: String?
    let ward: String?
    let shippingFee: Int?
    let voucherCode: String?
    let momoRequestType: String?
    let vnpayBankCode: String?
}

struct CartLine: Identifiable, Codable, Hashable {
    let id: Int
    let productId: Int
    let variantId: Int?
    let name: String
    let imageURL: String?
    let price: Int
    var quantity: Int
    var size: String?
    var color: String?
}

struct CartResponse: Codable {
    let cartId: Int?
    let userId: Int?
    let items: [CartItemResponse]?
    let totalPrice: Int?
}

struct CartItemResponse: Codable, Identifiable {
    let id: Int?
    let productId: Int?
    let productSlug: String?
    let productName: String?
    let productImage: String?
    let variantId: Int?
    let sku: String?
    let size: String?
    let color: String?
    let price: Int?
    let quantity: Int?
    let lineTotal: Int?
}

struct AddCartItemRequest: Codable {
    let variantId: Int
    let quantity: Int
}

struct UpdateCartItemRequest: Codable {
    let quantity: Int
}

struct WishlistResponse: Codable {
    let wishlistId: Int?
    let userId: Int?
    let productIds: [Int]?
}

struct ToggleWishlistItemRequest: Codable {
    let productId: Int
}

struct VoucherBestResponse: Codable {
    let code: String?
    let discountAmount: Int?
    let finalTotal: Int?
    let autoApplied: Bool?
}
