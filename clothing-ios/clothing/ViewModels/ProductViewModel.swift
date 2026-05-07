import Foundation
import Combine

class ProductViewModel: ObservableObject {
    @Published var products: [Product] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var searchText = ""
    
    // Phân trang
    @Published var currentPage = 0
    @Published var totalPages = 0
    @Published var totalElements = 0
    @Published var isLastPage = false
    
    private let pageSize = 10
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Debounce tìm kiếm: đợi 0.5s sau khi ngừng gõ rồi mới gọi API
        $searchText
            .debounce(for: .milliseconds(500), scheduler: RunLoop.main)
            .removeDuplicates()
            .sink { [weak self] _ in
                self?.refreshProducts()
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Tải sản phẩm
    
    func loadProducts(page: Int = 0) {
        isLoading = true
        errorMessage = nil
        
        ProductService.shared.getProducts(
            page: page,
            size: pageSize,
            query: searchText.isEmpty ? nil : searchText
        )
        .sink { [weak self] completion in
            self?.isLoading = false
            switch completion {
            case .failure(let error):
                self?.errorMessage = error.localizedDescription
            case .finished:
                break
            }
        } receiveValue: { [weak self] response in
            self?.isLoading = false
            
            if page == 0 {
                // Trang đầu: thay thế toàn bộ
                self?.products = response.content ?? []
            } else {
                // Trang tiếp theo: thêm vào cuối
                self?.products.append(contentsOf: response.content ?? [])
            }
            
            self?.currentPage = response.page ?? 0
            self?.totalPages = response.totalPages ?? 0
            self?.totalElements = response.totalElements ?? 0
            self?.isLastPage = response.last ?? true
        }
        .store(in: &cancellables)
    }
    
    // MARK: - Tải thêm (infinite scroll)
    
    func loadMore() {
        guard !isLoading, !isLastPage else { return }
        loadProducts(page: currentPage + 1)
    }
    
    // MARK: - Làm mới
    
    func refreshProducts() {
        currentPage = 0
        isLastPage = false
        products = []
        loadProducts(page: 0)
    }
}
