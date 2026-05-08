import Foundation
import Combine

@MainActor
final class CartStore: ObservableObject {
    @Published var items: [CartLine] = []
    private let key = "ios_cart_items"

    init() {
        load()
    }

    var subtotal: Int {
        items.reduce(0) { $0 + ($1.price * $1.quantity) }
    }

    func add(product: Product, variant: ProductVariant?) {
        guard let productId = product.id else { return }
        let variantId = variant?.id
        let price = variant?.price ?? product.minPrice ?? 0
        let name = product.name ?? "Product"
        let image = product.mainImageURL

        if let idx = items.firstIndex(where: { $0.productId == productId && $0.variantId == variantId }) {
            items[idx].quantity += 1
        } else {
            let newLine = CartLine(
                id: Int(Date().timeIntervalSince1970 * 1000),
                productId: productId,
                variantId: variantId,
                name: name,
                imageURL: image,
                price: price,
                quantity: 1
            )
            items.append(newLine)
        }
        save()
    }

    func remove(_ line: CartLine) {
        items.removeAll { $0.id == line.id }
        save()
    }

    func updateQuantity(_ line: CartLine, quantity: Int) {
        guard let idx = items.firstIndex(where: { $0.id == line.id }) else { return }
        items[idx].quantity = max(1, quantity)
        save()
    }

    func clear() {
        items.removeAll()
        save()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([CartLine].self, from: data) else { return }
        items = decoded
    }

    private func save() {
        if let encoded = try? JSONEncoder().encode(items) {
            UserDefaults.standard.set(encoded, forKey: key)
        }
    }
}
