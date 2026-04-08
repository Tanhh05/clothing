package com.clothing.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.clothing.dto.response.ProductSearchResponse;
import com.clothing.elasticsearch.document.ProductDocument;
import com.clothing.elasticsearch.repository.ProductSearchRepository;
import com.clothing.entity.CategoryEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.ProductImageEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.repository.CategoryRepository;
import com.clothing.repository.ProductImageRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.service.ProductSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final int MAX_SEARCH_SIZE = 50;

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    public ProductSearchServiceImpl(
            ProductSearchRepository productSearchRepository,
            ElasticsearchClient elasticsearchClient,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository
    ) {
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchClient = elasticsearchClient;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    public List<ProductSearchResponse> searchProducts(String keyword, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        int safeSize = Math.min(Math.max(size, 1), MAX_SEARCH_SIZE);
        try {
            SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .size(safeSize)
                            .query(q -> q.bool(buildSearchBoolQuery(normalizedKeyword))),
                    ProductDocument.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(doc -> doc != null)
                    .map(this::toResponse)
                    .toList();
        } catch (Exception ex) {
            log.warn("Product search failed for keyword={}", normalizedKeyword, ex);
            return List.of();
        }
    }

    @Override
    public void indexProduct(Long productId) {
        try {
            Optional<ProductEntity> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                removeProduct(productId);
                return;
            }
            productSearchRepository.save(toDocument(productOptional.get()));
        } catch (Exception ex) {
            log.warn("Failed to index product id={}", productId, ex);
        }
    }

    @Override
    public void removeProduct(Long productId) {
        try {
            productSearchRepository.deleteById(productId);
        } catch (Exception ex) {
            log.warn("Failed to remove product index id={}", productId, ex);
        }
    }

    @Override
    public int reindexAll() {
        List<ProductEntity> products = productRepository.findAllByOrderByIdDesc();
        if (products.isEmpty()) {
            return 0;
        }

        List<ProductDocument> documents = new ArrayList<>(products.size());
        for (ProductEntity product : products) {
            documents.add(toDocument(product));
        }
        productSearchRepository.saveAll(documents);
        return documents.size();
    }

    private BoolQuery buildSearchBoolQuery(String keyword) {
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);

        Query autocompleteByName = Query.of(q -> q.matchPhrasePrefix(m -> m
                .field("name")
                .query(keyword)
                .maxExpansions(20)));

        Query autocompleteByCategory = Query.of(q -> q.matchPhrasePrefix(m -> m
                .field("categoryName")
                .query(keyword)
                .maxExpansions(20)));

        MultiMatchQuery typoToleranceQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields("name^4", "brand^2", "categoryName^2", "description")
                .fuzziness("AUTO")
                .prefixLength(1)
                .operator(Operator.Or));

        Query fuzzy = Query.of(q -> q.multiMatch(typoToleranceQuery));
        Query slugFallback = Query.of(q -> q.wildcard(w -> w
                .field("slug")
                .value("*" + normalizedKeyword + "*")));

        return BoolQuery.of(b -> b
                .should(autocompleteByName)
                .should(autocompleteByCategory)
                .should(fuzzy)
                .should(slugFallback)
                .minimumShouldMatch("1"));
    }

    private ProductDocument toDocument(ProductEntity product) {
        CategoryEntity category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        List<ProductVariantEntity> variants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
        List<ProductImageEntity> images = productImageRepository.findByProductIdOrderByIdAsc(product.getId());

        Long minPrice = variants.stream()
                .map(ProductVariantEntity::getPrice)
                .filter(price -> price != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Long maxPrice = variants.stream()
                .map(ProductVariantEntity::getPrice)
                .filter(price -> price != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        String mainImageUrl = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .map(ProductImageEntity::getUrl)
                .findFirst()
                .orElseGet(() -> images.stream().map(ProductImageEntity::getUrl).findFirst().orElse(null));

        ProductDocument document = new ProductDocument();
        document.setId(product.getId());
        document.setName(product.getName());
        document.setSlug(product.getSlug());
        document.setDescription(product.getDescription());
        document.setBrand(product.getBrand());
        document.setCategoryId(product.getCategoryId());
        document.setCategoryName(category == null ? null : category.getName());
        document.setStatus(product.getStatus());
        document.setMainImageUrl(mainImageUrl);
        document.setMinPrice(minPrice);
        document.setMaxPrice(maxPrice);
        document.setCreatedAt(product.getCreatedAt() == null ? null : product.getCreatedAt().toString());
        return document;
    }

    private ProductSearchResponse toResponse(ProductDocument document) {
        return ProductSearchResponse.builder()
                .id(document.getId())
                .name(document.getName())
                .slug(document.getSlug())
                .brand(document.getBrand())
                .categoryName(document.getCategoryName())
                .mainImageUrl(document.getMainImageUrl())
                .minPrice(document.getMinPrice())
                .maxPrice(document.getMaxPrice())
                .build();
    }
}
