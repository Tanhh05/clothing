import Foundation
import Combine

@MainActor
final class WishlistStore: ObservableObject {
    @Published var productIds: Set<Int> = []
    private let key = "ios_wishlist_ids"

    init() {
        load()
    }

    func contains(_ productId: Int?) -> Bool {
        guard let productId else { return false }
        return productIds.contains(productId)
    }

    func toggle(_ productId: Int?) {
        guard let productId else { return }
        if productIds.contains(productId) {
            productIds.remove(productId)
        } else {
            productIds.insert(productId)
        }
        save()
    }

    private func load() {
        let ids = UserDefaults.standard.array(forKey: key) as? [Int] ?? []
        productIds = Set(ids)
    }

    private func save() {
        UserDefaults.standard.set(Array(productIds), forKey: key)
    }
}
