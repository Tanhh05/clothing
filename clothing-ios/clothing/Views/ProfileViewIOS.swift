import SwiftUI

struct ProfileViewIOS: View {
    @EnvironmentObject private var session: SessionStore
    private var displayName: String { TokenManager.shared.fullName ?? TokenManager.shared.username ?? "Khách hàng" }
    private var avatarURL: String {
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600&q=80"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    VStack(spacing: 10) {
                        ZStack(alignment: .bottomTrailing) {
                            AsyncImage(url: URL(string: avatarURL)) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Circle().fill(Color.gray.opacity(0.2))
                                }
                            }
                            .frame(width: 190, height: 190)
                            .clipShape(Circle())

                            Circle()
                                .fill(Color.black)
                                .frame(width: 48, height: 48)
                                .overlay(Image(systemName: "pencil").foregroundColor(.white))
                        }
                        Text(displayName)
                            .font(.system(size: 46, weight: .regular, design: .serif))
                        Text("Thành viên Platinum")
                            .font(.title3.weight(.medium))
                            .foregroundColor(Color(red: 0.45, green: 0.33, blue: 0.10))
                            .padding(.horizontal, 22)
                            .padding(.vertical, 8)
                            .background(Color(red: 0.94, green: 0.89, blue: 0.77))
                            .clipShape(Capsule())
                    }
                    .padding(.top, 8)

                    menuRow(icon: "bag", title: "Đơn hàng của tôi") {
                        OrdersView()
                    }
                    menuRow(icon: "heart", title: "Danh sách yêu thích") {
                        WishlistView()
                    }
                    menuRow(icon: "mappin.and.ellipse", title: "Địa chỉ giao hàng") {
                        CheckoutView()
                    }
                    menuRow(icon: "creditcard", title: "Phương thức thanh toán") {
                        Text("Sắp ra mắt")
                    }
                    menuRow(icon: "gearshape", title: "Cài đặt") {
                        Text("Sắp ra mắt")
                    }

                    Button {
                        session.logout()
                    } label: {
                        HStack {
                            Spacer()
                            Image(systemName: "arrow.right.square")
                            Text("Đăng xuất").fontWeight(.semibold)
                            Spacer()
                        }
                        .foregroundColor(.red)
                        .frame(height: 62)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red.opacity(0.7), lineWidth: 1))
                    }
                    .padding(.top, 8)
                }
                .padding(16)
            }
            .background(Color(white: 0.96))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image(systemName: "line.3.horizontal")
                }
                ToolbarItem(placement: .principal) {
                    Text("HỒ SƠ CỦA TÔI")
                        .font(.system(size: 28, weight: .black, design: .serif))
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Image(systemName: "bell")
                }
            }
        }
    }

    private func menuRow<Destination: View>(icon: String, title: String, @ViewBuilder destination: @escaping () -> Destination) -> some View {
        NavigationLink(destination: destination()) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.title2)
                    .frame(width: 50, height: 50)
                    .background(Color(white: 0.94))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                Text(title)
                    .font(.title2)
                    .foregroundColor(.black)
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray)
            }
            .padding(14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}
