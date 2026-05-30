import SwiftUI

struct RegisterView: View {
    @StateObject private var viewModel = AuthViewModel()
    @Environment(\.dismiss) var dismiss
    
    @State private var showSuccessAlert = false
    
    var body: some View {
        VStack(spacing: 20) {
            Text("Tạo Tài Khoản")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 20)
            
            VStack(spacing: 15) {
                TextField("Họ và tên", text: $viewModel.fullName)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)

                TextField("Tên đăng nhập", text: $viewModel.username)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .autocapitalization(.none)
                
                TextField("Email", text: $viewModel.email)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                
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
                    viewModel.register { success in
                        if success {
                            showSuccessAlert = true
                        }
                    }
                }) {
                    Text("Đăng ký")
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(10)
                }
                .padding(.horizontal)
            }
            
            Spacer()
        }
        .padding()
        .navigationTitle("Đăng Ký")
        .navigationBarTitleDisplayMode(.inline)
        .alert(isPresented: Binding(
            get: { viewModel.isError || showSuccessAlert },
            set: { _ in
                viewModel.isError = false
                showSuccessAlert = false
            }
        )) {
            if showSuccessAlert {
                return Alert(
                    title: Text("Thành Công"),
                    message: Text("Tài khoản đã được tạo thành công!"),
                    dismissButton: .default(Text("Về đăng nhập")) {
                        dismiss()
                    }
                )
            } else {
                return Alert(
                    title: Text("Lỗi"),
                    message: Text(viewModel.errorMessage ?? "Có lỗi xảy ra"),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }
}

#Preview {
    RegisterView()
}
