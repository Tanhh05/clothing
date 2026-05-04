package com.clothing.service;

import com.clothing.dto.response.ProductSearchResponse;

import java.util.List;

public interface ProductSearchService {

    List<ProductSearchResponse> searchProducts(String keyword, int size);

    void indexProduct(Long productId);

    void removeProduct(Long productId);

    int reindexAll();
}
