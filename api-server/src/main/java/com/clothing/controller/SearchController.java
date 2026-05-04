package com.clothing.controller;

import com.clothing.dto.response.ProductSearchResponse;
import com.clothing.service.ProductSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ProductSearchService productSearchService;

    public SearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductSearchResponse>> searchProducts(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productSearchService.searchProducts(keyword, size));
    }

    @PostMapping("/products/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reindexProducts() {
        int indexed = productSearchService.reindexAll();
        return ResponseEntity.ok(Map.of("indexed", indexed));
    }
}
