import Foundation
import Combine

@MainActor
final class CartStore: ObservableObject {
    @Published var items: [CartLine] = []
    @Published var isSyncing = false
    private let key = "ios_cart_items"

    init() {
        load()
        if TokenManager.shared.isLoggedIn {
            Task { await refreshFromServer() }
        }
    }

    var subtotal: Int {
        items.reduce(0) { $0 + ($1.price * $1.quantity) }
    }

    func add(product: Product, variant: ProductVariant?) {
        guard let productId = product.id else { return }
        let variantId = variant?.id
        if TokenManager.shared.isLoggedIn, let variantId {
            Task { [weak self] in
                guard let self else { return }
                do {
                    let cart = try await CartService.shared.addItem(variantId: variantId, quantity: 1)
                    self.applyServerCart(cart)
                } catch {
                    self.addLocal(productId: productId, variantId: variantId, product: product, variant: variant)
                }
            }
            return
        }
        addLocal(productId: productId, variantId: variantId, product: product, variant: variant)
    }

    private func addLocal(productId: Int, variantId: Int?, product: Product, variant: ProductVariant?) {
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
                quantity: 1,
                size: variant?.size,
                color: variant?.color
            )
            items.append(newLine)
        }
        save()
    }

    func remove(_ line: CartLine) {
        if TokenManager.shared.isLoggedIn {
            Task { [weak self] in
                guard let self else { return }
                do {
                    let cart = try await CartService.shared.removeItem(cartItemId: line.id)
                    self.applyServerCart(cart)
                } catch {
                    self.items.removeAll { $0.id == line.id }
                    self.save()
                }
            }
            return
        }
        items.removeAll { $0.id == line.id }
        save()
    }

    func updateQuantity(_ line: CartLine, quantity: Int) {
        let nextQty = max(1, quantity)
        if TokenManager.shared.isLoggedIn {
            Task { [weak self] in
                guard let self else { return }
                do {
                    let cart = try await CartService.shared.updateItem(cartItemId: line.id, quantity: nextQty)
                    self.applyServerCart(cart)
                } catch {
                    if let idx = self.items.firstIndex(where: { $0.id == line.id }) {
                        self.items[idx].quantity = nextQty
                        self.save()
                    }
                }
            }
            return
        }
        guard let idx = items.firstIndex(where: { $0.id == line.id }) else { return }
        items[idx].quantity = nextQty
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

    func refreshFromServer() async {
        guard TokenManager.shared.isLoggedIn else { return }
        isSyncing = true
        defer { isSyncing = false }
        do {
            let cart = try await CartService.shared.getMyCart()
            applyServerCart(cart)
        } catch {
            // Keep local snapshot on failures.
        }
    }

    private func applyServerCart(_ cart: CartResponse) {
        let mapped = (cart.items ?? []).map { item in
            CartLine(
                id: item.id ?? Int.random(in: 1...Int.max),
                productId: item.productId ?? 0,
                variantId: item.variantId,
                name: item.productName ?? "Sản phẩm",
                imageURL: item.productImage,
                price: item.price ?? 0,
                quantity: item.quantity ?? 1,
                size: item.size,
                color: item.color
            )
        }
        items = mapped
        save()
    }
}
