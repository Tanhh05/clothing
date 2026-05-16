import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("Trang chủ", systemImage: "house")
                }

            SearchView()
                .tabItem {
                    Label("Tìm kiếm", systemImage: "magnifyingglass")
                }

            CartView()
                .tabItem {
                    Label("Giỏ hàng", systemImage: "cart")
                }

            ProfileViewIOS()
                .tabItem {
                    Label("Tài khoản", systemImage: "person")
                }
        }
    }
}
