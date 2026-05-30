import Foundation

enum AppConfig {
    // Simulator dùng localhost, thiết bị thật phải dùng IP LAN của máy chạy backend.
    #if targetEnvironment(simulator)
    static let backendBaseURL = "http://127.0.0.1:8080"
    #else
    static let backendBaseURL = "http://192.168.1.10:8080" // TODO: đổi theo IP máy bạn
    #endif
    static let storefrontBaseURL = "http://localhost:3001"
}
