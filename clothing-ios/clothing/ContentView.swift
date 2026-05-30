//
//  ContentView.swift
//  clothing
//
//  Created by tanh on 3/26/26.
//

import SwiftUI

struct ContentView: View {
    @StateObject private var session = SessionStore()
    @StateObject private var cartStore = CartStore()
    @StateObject private var wishlistStore = WishlistStore()
    @AppStorage("accessToken") private var accessToken: String = ""

    var body: some View {
        if !accessToken.isEmpty {
            MainTabView()
                .environmentObject(session)
                .environmentObject(cartStore)
                .environmentObject(wishlistStore)
        } else {
            LoginView()
        }
    }
}

#Preview {
    ContentView()
}
