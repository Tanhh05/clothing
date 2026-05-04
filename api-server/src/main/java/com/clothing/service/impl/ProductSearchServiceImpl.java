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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.io.IOException;

@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final int MAX_SEARCH_SIZE = 50;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final SearchPerformanceTracker searchPerformanceTracker;
    private final boolean elasticsearchEnabled;
    private final int searchTimeoutMs;
    private final int fallbackMaxSize;

    public ProductSearchServiceImpl(
            ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider,
            ObjectProvider<ElasticsearchClient> elasticsearchClientProvider,
            ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            SearchPerformanceTracker searchPerformanceTracker,
            @Value("${app.integrations.elasticsearch.enabled:true}") boolean elasticsearchEnabled,
            @Value("${app.search.timeout-ms:400}") int searchTimeoutMs,
            @Value("${app.search.fallback-max-size:20}") int fallbackMaxSize
    ) {
        this.productSearchRepository = productSearchRepositoryProvider.getIfAvailable();
        this.elasticsearchClient = elasticsearchClientProvider.getIfAvailable();
        this.elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.searchPerformanceTracker = searchPerformanceTracker;
        this.elasticsearchEnabled = elasticsearchEnabled;
        this.searchTimeoutMs = Math.max(100, searchTimeoutMs);
        this.fallbackMaxSize = Math.max(5, fallbackMaxSize);
    }

    @PostConstruct
    public void ensureProductIndexAtStartup() {
        if (!isElasticsearchAvailable()) {
            return;
        }
        ensureIndexExists();
    }

    @Override
    public List<ProductSearchResponse> searchProducts(String keyword, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        int safeSize = Math.min(Math.max(size, 1), MAX_SEARCH_SIZE);
        long startedAt = System.nanoTime();
        if (!isElasticsearchAvailable()) {
            List<ProductSearchResponse> fallback = searchProductsFromDbFallback(normalizedKeyword, safeSize);
            searchPerformanceTracker.record(elapsedMs(startedAt), "db-fallback", fallback.size());
            return fallback;
        }
        try {
            List<ProductSearchResponse> results = CompletableFuture
                    .supplyAsync(() -> executeElasticsearchSearch(normalizedKeyword, safeSize))
                    .get(searchTimeoutMs, TimeUnit.MILLISECONDS);
            searchPerformanceTracker.record(elapsedMs(startedAt), "elasticsearch", results.size());
            return results;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            List<ProductSearchResponse> fallback = searchProductsFromDbFallback(normalizedKeyword, safeSize);
            searchPerformanceTracker.record(elapsedMs(startedAt), "db-fallback", fallback.size());
            return fallback;
        } catch (TimeoutException | ExecutionException ex) {
            log.warn("Product search fallback to DB for keyword={} cause={}", normalizedKeyword, ex.getClass().getSimpleName());
            List<ProductSearchResponse> fallback = searchProductsFromDbFallback(normalizedKeyword, safeSize);
            searchPerformanceTracker.record(elapsedMs(startedAt), "db-fallback", fallback.size());
            return fallback;
        } catch (Exception ex) {
            log.warn("Product search fallback to DB for keyword={}", normalizedKeyword, ex);
            List<ProductSearchResponse> fallback = searchProductsFromDbFallback(normalizedKeyword, safeSize);
            searchPerformanceTracker.record(elapsedMs(startedAt), "db-fallback", fallback.size());
            return fallback;
        }
    }

    @Override
    public void indexProduct(Long productId) {
        if (!isElasticsearchAvailable()) {
            return;
        }
        try {
            ensureIndexExists();
            Optional<ProductEntity> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                removeProduct(productId);
                return;
            }
            ProductEntity product = productOptional.get();
            if (!shouldIndex(product)) {
                removeProduct(productId);
                return;
            }
            ProductIndexContext context = buildIndexContext(List.of(product));
            productSearchRepository.save(toDocument(product, context));
        } catch (Exception ex) {
            log.warn("Failed to index product id={}", productId, ex);
        }
    }

    @Override
    public void removeProduct(Long productId) {
        if (!isElasticsearchAvailable()) {
            return;
        }
        try {
            productSearchRepository.deleteById(productId);
        } catch (Exception ex) {
            log.warn("Failed to remove product index id={}", productId, ex);
        }
    }

    @Override
    public int reindexAll() {
        if (!isElasticsearchAvailable()) {
            return 0;
        }
        List<ProductEntity> products = productRepository.findAllByOrderByIdDesc().stream()
                .filter(this::shouldIndex)
                .toList();
        recreateIndex();
        if (products.isEmpty()) {
            return 0;
        }

        ProductIndexContext context = buildIndexContext(products);
        List<ProductDocument> documents = new ArrayList<>(products.size());
        for (ProductEntity product : products) {
            documents.add(toDocument(product, context));
        }
        productSearchRepository.saveAll(documents);
        return documents.size();
    }

    private BoolQuery buildSearchBoolQuery(String keyword) {
        Query autocompleteByName = Query.of(q -> q.matchPhrasePrefix(m -> m
                .field("name")
                .query(keyword)
                .maxExpansions(20)));

        Query autocompleteByCategory = Query.of(q -> q.matchPhrasePrefix(m -> m
                .field("categoryName")
                .query(keyword)
                .maxExpansions(20)));

        MultiMatchQuery matchQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields("name^5", "brand^2", "categoryName^2", "description")
                .operator(Operator.Or));
        Query fullText = Query.of(q -> q.multiMatch(matchQuery));

        BoolQuery.Builder queryBuilder = new BoolQuery.Builder()
                .must(Query.of(q -> q.term(t -> t.field("status").value(STATUS_ACTIVE))))
                .should(autocompleteByName)
                .should(autocompleteByCategory)
                .should(fullText)
                .minimumShouldMatch("1");

        if (keyword.length() >= 4) {
            MultiMatchQuery typoToleranceQuery = MultiMatchQuery.of(m -> m
                    .query(keyword)
                    .fields("name^4", "brand^2", "categoryName^2")
                    .fuzziness("AUTO")
                    .prefixLength(1)
                    .operator(Operator.Or));
            queryBuilder.should(Query.of(q -> q.multiMatch(typoToleranceQuery)));
        }
        return queryBuilder.build();
    }

    private List<ProductSearchResponse> executeElasticsearchSearch(String keyword, int size) {
        try {
            SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .size(size)
                            .query(q -> q.bool(buildSearchBoolQuery(keyword))),
                    ProductDocument.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(doc -> doc != null)
                    .map(this::toResponse)
                    .toList();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<ProductSearchResponse> searchProductsFromDbFallback(String keyword, int size) {
        int fallbackSize = Math.min(Math.max(size, 1), fallbackMaxSize);
        List<ProductEntity> products = productRepository.searchActiveForFallback(
                keyword.toLowerCase(Locale.ROOT),
                PageRequest.of(0, fallbackSize)
        );
        if (products.isEmpty()) {
            return List.of();
        }

        ProductIndexContext context = buildIndexContext(products);
        return products.stream()
                .map(product -> toResponse(toDocument(product, context)))
                .toList();
    }

    private ProductDocument toDocument(ProductEntity product, ProductIndexContext context) {
        CategoryEntity category = context.categoriesById().get(product.getCategoryId());
        List<ProductVariantEntity> variants = context.variantsByProductId().getOrDefault(product.getId(), List.of());
        List<ProductImageEntity> images = context.imagesByProductId().getOrDefault(product.getId(), List.of());

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

    private ProductIndexContext buildIndexContext(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return new ProductIndexContext(Map.of(), Map.of(), Map.of());
        }
        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        Set<Long> categoryIds = products.stream()
                .map(ProductEntity::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, category -> category));

        Map<Long, List<ProductVariantEntity>> variantsByProductId = productVariantRepository
                .findByProductIdInOrderByProductIdAscIdAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductVariantEntity::getProductId));

        Map<Long, List<ProductImageEntity>> imagesByProductId = productImageRepository
                .findByProductIdInOrderByProductIdAscIdAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductImageEntity::getProductId));

        return new ProductIndexContext(categoriesById, variantsByProductId, imagesByProductId);
    }

    private void ensureIndexExists() {
        if (!isElasticsearchAvailable()) {
            return;
        }
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            return;
        }
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping(ProductDocument.class));
    }

    private void recreateIndex() {
        if (!isElasticsearchAvailable()) {
            return;
        }
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping(ProductDocument.class));
    }

    private boolean shouldIndex(ProductEntity product) {
        return !Boolean.TRUE.equals(product.getDeleted())
                && STATUS_ACTIVE.equalsIgnoreCase(product.getStatus());
    }

    private long elapsedMs(long startedAtNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNs);
    }

    private boolean isElasticsearchAvailable() {
        return elasticsearchEnabled
                && productSearchRepository != null
                && elasticsearchClient != null
                && elasticsearchOperations != null;
    }

    private record ProductIndexContext(
            Map<Long, CategoryEntity> categoriesById,
            Map<Long, List<ProductVariantEntity>> variantsByProductId,
            Map<Long, List<ProductImageEntity>> imagesByProductId
    ) {
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
