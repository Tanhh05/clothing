import Foundation
import Combine

@MainActor
final class SessionStore: ObservableObject {
    @Published var isLoggedIn: Bool
    private var cancellables = Set<AnyCancellable>()

    init() {
        self.isLoggedIn = TokenManager.shared.isLoggedIn
        NotificationCenter.default.publisher(for: .authStateDidChange)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.reload()
            }
            .store(in: &cancellables)
    }

    func reload() {
        isLoggedIn = TokenManager.shared.isLoggedIn
    }

    func logout() {
        TokenManager.shared.clearAll()
        isLoggedIn = false
    }
}
