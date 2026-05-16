import Foundation
import Combine

@MainActor
final class WishlistStore: ObservableObject {
    @Published var productIds: Set<Int> = []
    @Published var isSyncing = false
    private let key = "ios_wishlist_ids"

    init() {
        load()
        if TokenManager.shared.isLoggedIn {
            Task { await refreshFromServer() }
        }
    }

    func contains(_ productId: Int?) -> Bool {
        guard let productId else { return false }
        return productIds.contains(productId)
    }

    func toggle(_ productId: Int?) {
        guard let productId else { return }
        if TokenManager.shared.isLoggedIn {
            Task { [weak self] in
                guard let self else { return }
                do {
                    if self.productIds.contains(productId) {
                        let response = try await WishlistService.shared.removeItem(productId: productId)
                        self.productIds = Set(response.productIds ?? [])
                    } else {
                        let response = try await WishlistService.shared.addItem(productId: productId)
                        self.productIds = Set(response.productIds ?? [])
                    }
                    self.save()
                } catch {
                    self.toggleLocal(productId)
                }
            }
            return
        }
        toggleLocal(productId)
    }

    private func toggleLocal(_ productId: Int) {
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

    func refreshFromServer() async {
        guard TokenManager.shared.isLoggedIn else { return }
        isSyncing = true
        defer { isSyncing = false }
        do {
            let response = try await WishlistService.shared.getMyWishlist()
            productIds = Set(response.productIds ?? [])
            save()
        } catch {
            // keep local state
        }
    }
}
