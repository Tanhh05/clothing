import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = AuthViewModel()
    @State private var showRegister = false
    
    // Closure để thông báo ContentView rằng user đã login thành công
    var onLoginSuccess: (() -> Void)?
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("Chào Mừng")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.bottom, 30)
                
                VStack(spacing: 15) {
                    TextField("Tên đăng nhập hoặc Email", text: $viewModel.usernameOrEmail)
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    
                    SecureField("Mật khẩu", text: $viewModel.password)
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                
                if viewModel.isLoading {
                    ProgressView()
                        .padding()
                } else {
                    Button(action: {
                        viewModel.login { success in
                            if success {
                                onLoginSuccess?()
                            }
                        }
                    }) {
                        Text("Đăng nhập")
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .cornerRadius(10)
                    }
                    .padding(.horizontal)
                }
                
                HStack {
                    Text("Chưa có tài khoản?")
                    Button("Đăng ký ngay") {
                        showRegister = true
                    }
                }
                .padding(.top, 10)
                
                Spacer()
            }
            .padding()
            .navigationDestination(isPresented: $showRegister) {
                RegisterView()
            }
            .alert(isPresented: $viewModel.isError) {
                Alert(
                    title: Text("Lỗi"),
                    message: Text(viewModel.errorMessage ?? "Có lỗi xảy ra"),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }
}

#Preview {
    LoginView()
}
