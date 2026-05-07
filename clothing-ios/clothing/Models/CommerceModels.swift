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
}

struct CreateOrderRequest: Codable {
    let paymentMethod: String
    let address: String
    let phone: String
    let customerName: String
    let customerEmail: String
    let notes: String?
}

struct CartLine: Identifiable, Codable, Hashable {
    let id: Int
    let productId: Int
    let variantId: Int?
    let name: String
    let imageURL: String?
    let price: Int
    var quantity: Int
}

