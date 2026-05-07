import Foundation
import Combine

@MainActor
final class SessionStore: ObservableObject {
    @Published var isLoggedIn: Bool

    init() {
        self.isLoggedIn = TokenManager.shared.isLoggedIn
    }

    func reload() {
        isLoggedIn = TokenManager.shared.isLoggedIn
    }

    func logout() {
        TokenManager.shared.clearAll()
        isLoggedIn = false
    }
}
