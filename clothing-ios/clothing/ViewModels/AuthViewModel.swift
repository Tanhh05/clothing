import Foundation
import Combine
import SwiftUI

class AuthViewModel: ObservableObject {
    @Published var usernameOrEmail = ""
    @Published var username = ""
    @Published var fullName = ""
    @Published var email = ""
    @Published var password = ""
    
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var isError = false
    @Published var isAuthenticated = false
    
    // Thông tin user sau khi đăng nhập
    @Published var currentUsername: String? = nil
    @Published var currentEmail: String? = nil
    @Published var currentFullName: String? = nil
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Kiểm tra nếu đã đăng nhập trước đó
        if TokenManager.shared.isLoggedIn {
            isAuthenticated = true
            currentUsername = TokenManager.shared.username
            currentEmail = TokenManager.shared.email
            currentFullName = TokenManager.shared.fullName
        }
    }
    
    // MARK: - Đăng nhập
    
    func login(onComplete: ((Bool) -> Void)? = nil) {
        let normalizedUsernameOrEmail = usernameOrEmail.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedUsernameOrEmail.isEmpty, !password.isEmpty else {
            self.errorMessage = "Vui lòng nhập đầy đủ thông tin."
            self.isError = true
            onComplete?(false)
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        let request = LoginRequest(usernameOrEmail: normalizedUsernameOrEmail, password: password)
        
        AuthService.shared.login(request: request)
            .sink { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    self?.errorMessage = error.localizedDescription
                    self?.isError = true
                    onComplete?(false)
                case .finished:
                    break
                }
            } receiveValue: { [weak self] response in
                self?.isLoading = false
                
                // Lưu token và thông tin user
                TokenManager.shared.saveAuthResponse(response)
                
                // Cập nhật thông tin hiển thị
                self?.currentUsername = response.username
                self?.currentEmail = response.email
                self?.currentFullName = response.fullName
                
                self?.isAuthenticated = true
                onComplete?(true)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Đăng ký
    
    func register(completionHandler: @escaping (Bool) -> Void) {
        let normalizedFullName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedFullName.isEmpty, !normalizedUsername.isEmpty, !normalizedEmail.isEmpty, !password.isEmpty else {
            self.errorMessage = "Vui lòng nhập đầy đủ thông tin."
            self.isError = true
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        let request = RegisterRequest(
            fullName: normalizedFullName,
            username: normalizedUsername,
            email: normalizedEmail,
            password: password
        )
        
        AuthService.shared.register(request: request)
            .sink { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    self?.errorMessage = error.localizedDescription
                    self?.isError = true
                    completionHandler(false)
                case .finished:
                    break
                }
            } receiveValue: { [weak self] response in
                self?.isLoading = false
                completionHandler(true)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Đăng xuất
    
    func logout() {
        isLoading = true
        
        AuthService.shared.logout()
            .sink { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure:
                    // Vẫn đăng xuất local ngay cả khi server lỗi
                    TokenManager.shared.clearAll()
                    self?.isAuthenticated = false
                case .finished:
                    break
                }
            } receiveValue: { [weak self] _ in
                self?.isLoading = false
                self?.isAuthenticated = false
                self?.currentUsername = nil
                self?.currentEmail = nil
                self?.currentFullName = nil
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Refresh Token
    
    func refreshToken() {
        AuthService.shared.refreshToken()
            .sink { [weak self] completion in
                switch completion {
                case .failure:
                    // Token hết hạn, yêu cầu đăng nhập lại
                    TokenManager.shared.clearAll()
                    self?.isAuthenticated = false
                case .finished:
                    break
                }
            } receiveValue: { response in
                TokenManager.shared.saveAuthResponse(response)
            }
            .store(in: &cancellables)
    }
}
