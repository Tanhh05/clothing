package com.clothing.service.impl;

import com.clothing.dto.request.ProductImageRequest;
import com.clothing.dto.request.ProductUpsertRequest;
import com.clothing.dto.request.ProductVariantRequest;
import com.clothing.dto.response.InventoryAlertResponse;
import com.clothing.dto.response.InventoryLogResponse;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.ProductImportResponse;
import com.clothing.dto.response.ProductImportRowResultResponse;
import com.clothing.dto.response.ProductImageResponse;
import com.clothing.dto.response.ProductResponse;
import com.clothing.dto.response.ProductVariantResponse;
import com.clothing.entity.AttributeEntity;
import com.clothing.entity.AttributeValueEntity;
import com.clothing.entity.CategoryEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.ProductImageEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.VariantAttributeValueEntity;
import com.clothing.entity.InventoryLogEntity;
import com.clothing.exception.BusinessException;
import com.clothing.config.CacheConfig;
import com.clothing.repository.AttributeRepository;
import com.clothing.repository.AttributeValueRepository;
import com.clothing.repository.CategoryRepository;
import com.clothing.repository.ProductImageRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.ReviewRepository;
import com.clothing.repository.VariantAttributeValueRepository;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.service.ProductService;
import com.clothing.service.ProductSearchService;
import com.clothing.service.AuditLogService;
import com.clothing.service.InventoryMovementService;
import com.clothing.service.WishlistService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.clothing.config.RequestMetaResolver;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ATTRIBUTE_SIZE = "Size";
    private static final String ATTRIBUTE_COLOR = "Color";
    private static final int IMPORT_COL_NAME = 0;
    private static final int IMPORT_COL_BRAND = 1;
    private static final int IMPORT_COL_DESCRIPTION = 2;
    private static final int IMPORT_COL_CATEGORY = 3;
    private static final int IMPORT_COL_STATUS = 4;
    private static final int IMPORT_COL_IMAGE_URL = 5;
    private static final int IMPORT_COL_SKU = 6;
    private static final int IMPORT_COL_PRICE = 7;
    private static final int IMPORT_COL_STOCK = 8;
    private static final int IMPORT_COL_WEIGHT = 9;
    private static final int IMPORT_COL_SIZE = 10;
    private static final int IMPORT_COL_COLOR = 11;
    private static final int IMPORT_COL_SLUG = 12;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final ProductSearchService productSearchService;
    private final AuditLogService auditLogService;
    private final InventoryMovementService inventoryMovementService;
    private final WishlistService wishlistService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            ReviewRepository reviewRepository,
            InventoryLogRepository inventoryLogRepository,
            AttributeRepository attributeRepository,
            AttributeValueRepository attributeValueRepository,
            VariantAttributeValueRepository variantAttributeValueRepository,
            ProductSearchService productSearchService,
            AuditLogService auditLogService,
            InventoryMovementService inventoryMovementService,
            WishlistService wishlistService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.reviewRepository = reviewRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.variantAttributeValueRepository = variantAttributeValueRepository;
        this.productSearchService = productSearchService;
        this.auditLogService = auditLogService;
        this.inventoryMovementService = inventoryMovementService;
        this.wishlistService = wishlistService;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    public ProductResponse create(ProductUpsertRequest request) {
        return createInternal(request);
    }

    private ProductResponse createInternal(ProductUpsertRequest request) {
        validateUpsertRequest(request);
        CategoryEntity category = getCategory(request.getCategoryId());
        String slug = resolveSlug(request.getSlug(), request.getName(), null);
        validateVariantSkus(request.getVariants(), null);
        String normalizedName = request.getName().trim();
        String resolvedNameVi = normalizeOptionalText(request.getNameVi(), normalizedName);
        String resolvedNameEn = normalizeOptionalText(request.getNameEn(), normalizedName);
        String resolvedNameMy = normalizeOptionalText(request.getNameMy(), resolvedNameVi);
        String resolvedDescriptionVi = normalizeOptionalText(request.getDescriptionVi(), request.getDescription());
        String resolvedDescriptionEn = normalizeOptionalText(request.getDescriptionEn(), request.getDescription());
        String resolvedDescriptionMy = normalizeOptionalText(request.getDescriptionMy(), resolvedDescriptionVi);

        ProductEntity product = new ProductEntity();
        product.setName(normalizedName);
        product.setNameVi(resolvedNameVi);
        product.setNameEn(resolvedNameEn);
        product.setNameMy(resolvedNameMy);
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setDescriptionVi(resolvedDescriptionVi);
        product.setDescriptionEn(resolvedDescriptionEn);
        product.setDescriptionMy(resolvedDescriptionMy);
        product.setBrand(request.getBrand());
        product.setCategoryId(category.getId());
        product.setStatus(normalizeStatus(request.getStatus()));
        ProductEntity savedProduct = productRepository.save(product);

        saveImages(savedProduct.getId(), request.getImages());
        saveVariants(savedProduct.getId(), request.getVariants());
        productSearchService.indexProduct(savedProduct.getId());
        auditLogService.log(
                "PRODUCT_CREATED",
                "PRODUCT",
                savedProduct.getId(),
                "Created product: " + savedProduct.getName()
        );

        return getById(savedProduct.getId());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL_CACHE, key = "#id")
    })
    public ProductResponse update(Long id, ProductUpsertRequest request) {
        validateUpsertRequest(request);
        ProductEntity product = findActiveProductById(id);
        CategoryEntity category = getCategory(request.getCategoryId());
        String slug = resolveSlug(request.getSlug(), request.getName(), id);
        validateVariantSkus(request.getVariants(), id);
        Long oldMinPrice = findCurrentMinPrice(id);
        String normalizedName = request.getName().trim();
        String resolvedNameVi = normalizeOptionalText(request.getNameVi(), normalizedName);
        String resolvedNameEn = normalizeOptionalText(request.getNameEn(), normalizedName);
        String resolvedNameMy = normalizeOptionalText(request.getNameMy(), resolvedNameVi);
        String resolvedDescriptionVi = normalizeOptionalText(request.getDescriptionVi(), request.getDescription());
        String resolvedDescriptionEn = normalizeOptionalText(request.getDescriptionEn(), request.getDescription());
        String resolvedDescriptionMy = normalizeOptionalText(request.getDescriptionMy(), resolvedDescriptionVi);

        product.setName(normalizedName);
        product.setNameVi(resolvedNameVi);
        product.setNameEn(resolvedNameEn);
        product.setNameMy(resolvedNameMy);
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setDescriptionVi(resolvedDescriptionVi);
        product.setDescriptionEn(resolvedDescriptionEn);
        product.setDescriptionMy(resolvedDescriptionMy);
        product.setBrand(request.getBrand());
        product.setCategoryId(category.getId());
        product.setStatus(normalizeStatus(request.getStatus()));
        productRepository.save(product);

        productImageRepository.deleteByProductId(id);
        productImageRepository.flush();

        saveImages(id, request.getImages());
        upsertVariants(id, request.getVariants());
        Long newMinPrice = findCurrentMinPrice(id);
        wishlistService.notifyPriceDrop(id, oldMinPrice, newMinPrice);
        productSearchService.indexProduct(id);
        auditLogService.log(
                "PRODUCT_UPDATED",
                "PRODUCT",
                id,
                "Updated product: " + product.getName()
        );

        return getById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL_CACHE, key = "#id")
    })
    public void delete(Long id) {
        ProductEntity product = findActiveProductById(id);
        product.setDeleted(true);
        product.setStatus("INACTIVE");
        productRepository.save(product);
        productSearchService.removeProduct(id);
        auditLogService.log(
                "PRODUCT_SOFT_DELETED",
                "PRODUCT",
                id,
                "Soft deleted product: " + product.getName()
        );
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.PRODUCT_DETAIL_CACHE,
            key = "#id + ':l:' + T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public ProductResponse getById(Long id) {
        ProductEntity product = findActiveProductById(id);
        return toResponse(product, true);
    }

    @Override
    public ProductResponse getBySlugOrId(String productKey) {
        if (productKey == null || productKey.isBlank()) {
            throw new BusinessException("Product key is required", HttpStatus.BAD_REQUEST);
        }

        ProductEntity bySlug = productRepository.findBySlugIgnoreCase(productKey.trim()).orElse(null);
        if (bySlug != null && !Boolean.TRUE.equals(bySlug.getDeleted())) {
            return toResponse(bySlug, true);
        }

        try {
            Long id = Long.valueOf(productKey.trim());
            return getById(id);
        } catch (NumberFormatException ex) {
            throw new BusinessException("Product not found", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.PRODUCT_LIST_CACHE,
            key = "'p:' + #page + ':s:' + #size + ':sb:' + #sortBy + ':d:' + #direction + ':c:' + #category + ':q:' + #q + ':l:' + T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public PageResponse<ProductResponse> getAll(int page, int size, String sortBy, String direction, Long category, String q) {
        if (page < 0) {
            throw new BusinessException("page must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException("size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSortBy));
        Specification<ProductEntity> specification = Specification.where(
                (root, query, cb) -> cb.or(
                        cb.isNull(root.get("deleted")),
                        cb.isFalse(root.get("deleted"))
                )
        );

        if (category != null) {
            Set<Long> categoryIds = resolveCategoryTreeIds(category);
            if (categoryIds.isEmpty()) {
                return PageResponse.<ProductResponse>builder()
                        .content(List.of())
                        .page(page)
                        .size(size)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build();
            }
            specification = specification.and((root, query, cb) -> root.get("categoryId").in(categoryIds));
        }

        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> {
                Predicate byName = cb.like(cb.lower(root.get("name")), keyword);
                Predicate bySlug = cb.like(cb.lower(root.get("slug")), keyword);
                Predicate byBrand = cb.like(cb.lower(root.get("brand")), keyword);
                return cb.or(byName, bySlug, byBrand);
            });
        }

        Page<ProductEntity> productPage = productRepository.findAll(specification, pageable);

        return PageResponse.<ProductResponse>builder()
                .content(productPage.getContent().stream().map(p -> toResponse(p, false)).toList())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    @Override
    public List<ProductResponse> getRecommendations(List<Long> productIds, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Long> baseIds = productIds == null ? List.of() : productIds.stream().filter(id -> id != null && id > 0).toList();
        if (baseIds.isEmpty()) {
            return productRepository.findAllByOrderByIdDesc().stream()
                    .filter(product -> !Boolean.TRUE.equals(product.getDeleted()))
                    .filter(product -> "ACTIVE".equalsIgnoreCase(String.valueOf(product.getStatus())))
                    .limit(safeLimit)
                    .map(product -> toResponse(product, false))
                    .toList();
        }

        Set<Long> categoryIds = new LinkedHashSet<>();
        for (Long productId : baseIds) {
            ProductEntity product = productRepository.findById(productId).orElse(null);
            if (product != null && product.getCategoryId() != null) {
                categoryIds.add(product.getCategoryId());
            }
        }
        Set<Long> excluded = new HashSet<>(baseIds);

        List<ProductEntity> pool = productRepository.findAllByOrderByIdDesc();
        return pool.stream()
                .filter(product -> !Boolean.TRUE.equals(product.getDeleted()))
                .filter(product -> "ACTIVE".equalsIgnoreCase(String.valueOf(product.getStatus())))
                .filter(product -> !excluded.contains(product.getId()))
                .sorted((a, b) -> {
                    boolean aMatch = categoryIds.contains(a.getCategoryId());
                    boolean bMatch = categoryIds.contains(b.getCategoryId());
                    if (aMatch != bMatch) return aMatch ? -1 : 1;
                    return Long.compare(b.getId(), a.getId());
                })
                .limit(safeLimit)
                .map(product -> toResponse(product, false))
                .toList();
    }

    @Override
    public List<String> getVariantOptions(String type) {
        AttributeEntity attribute = getOrCreateAttribute(resolveVariantAttributeName(type));
        List<AttributeValueEntity> values = attributeValueRepository.findByAttributeIdOrderByValueAsc(attribute.getId());

        Set<String> unique = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> result = new ArrayList<>();
        for (AttributeValueEntity value : values) {
            String normalized = value.getValue() == null ? "" : value.getValue().trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (unique.add(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public String createVariantOption(String type, String value) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            throw new BusinessException("value is required", HttpStatus.BAD_REQUEST);
        }
        AttributeEntity attribute = getOrCreateAttribute(resolveVariantAttributeName(type));
        AttributeValueEntity entity = getOrCreateAttributeValue(attribute.getId(), normalizedValue);
        return entity.getValue();
    }

    @Override
    public List<ProductResponse> getDeletedProducts() {
        return productRepository.findByDeletedTrueOrderByIdDesc()
                .stream()
                .map(product -> toResponse(product, false))
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL_CACHE, key = "#id")
    })
    public ProductResponse restore(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException("Product is not deleted", HttpStatus.BAD_REQUEST);
        }
        product.setDeleted(false);
        if (product.getStatus() == null || product.getStatus().isBlank() || "INACTIVE".equalsIgnoreCase(product.getStatus())) {
            product.setStatus(STATUS_ACTIVE);
        }
        productRepository.save(product);
        productSearchService.indexProduct(id);
        auditLogService.log(
                "PRODUCT_RESTORED",
                "PRODUCT",
                id,
                "Restored product: " + product.getName()
        );
        return toResponse(product, true);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    public int bulkDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        int affected = 0;
        for (Long id : ids) {
            ProductEntity product = productRepository.findById(id).orElse(null);
            if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
                continue;
            }
            product.setDeleted(true);
            product.setStatus("INACTIVE");
            productRepository.save(product);
            productSearchService.removeProduct(product.getId());
            affected += 1;
        }
        auditLogService.log("PRODUCT_BULK_SOFT_DELETE", "PRODUCT", null, "Affected products: " + affected);
        return affected;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    public int bulkRestore(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        int affected = 0;
        for (Long id : ids) {
            ProductEntity product = productRepository.findById(id).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getDeleted())) {
                continue;
            }
            product.setDeleted(false);
            if (product.getStatus() == null || product.getStatus().isBlank() || "INACTIVE".equalsIgnoreCase(product.getStatus())) {
                product.setStatus(STATUS_ACTIVE);
            }
            productRepository.save(product);
            productSearchService.indexProduct(product.getId());
            affected += 1;
        }
        auditLogService.log("PRODUCT_BULK_RESTORE", "PRODUCT", null, "Affected products: " + affected);
        return affected;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    public int bulkUpdateStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (status == null || status.isBlank()) {
            throw new BusinessException("status is required", HttpStatus.BAD_REQUEST);
        }
        String normalizedStatus = normalizeStatus(status);
        int affected = 0;
        for (Long id : ids) {
            ProductEntity product = productRepository.findById(id).orElse(null);
            if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
                continue;
            }
            product.setStatus(normalizedStatus);
            productRepository.save(product);
            if ("ACTIVE".equalsIgnoreCase(normalizedStatus)) {
                productSearchService.indexProduct(product.getId());
            } else {
                productSearchService.removeProduct(product.getId());
            }
            affected += 1;
        }
        auditLogService.log(
                "PRODUCT_BULK_STATUS",
                "PRODUCT",
                null,
                "Set status " + normalizedStatus + " for " + affected + " products"
        );
        return affected;
    }

    @Override
    public List<InventoryAlertResponse> getLowStockAlerts(int threshold) {
        int safeThreshold = Math.max(0, threshold);
        return productVariantRepository.findByStockLessThanEqualOrderByStockAsc(safeThreshold)
                .stream()
                .map(variant -> {
                    ProductEntity product = productRepository.findById(variant.getProductId()).orElse(null);
                    if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
                        return null;
                    }
                    return InventoryAlertResponse.builder()
                            .variantId(variant.getId())
                            .sku(variant.getSku())
                            .stock(variant.getStock())
                            .productId(product.getId())
                            .productName(product.getName())
                            .build();
                })
                .filter(item -> item != null)
                .toList();
    }

    @Override
    public List<InventoryLogResponse> getInventoryLogs(Long variantId) {
        List<InventoryLogEntity> logs = variantId == null
                ? inventoryLogRepository.findTop200ByOrderByIdDesc()
                : inventoryLogRepository.findTop100ByVariantIdOrderByIdDesc(variantId);

        return logs.stream()
                .map(log -> {
                    ProductVariantEntity variant = productVariantRepository.findById(log.getVariantId()).orElse(null);
                    return InventoryLogResponse.builder()
                            .id(log.getId())
                            .variantId(log.getVariantId())
                            .sku(variant == null ? null : variant.getSku())
                            .type(log.getType())
                            .quantity(log.getQuantity())
                            .beforeStock(log.getBeforeStock())
                            .afterStock(log.getAfterStock())
                            .note(log.getNote())
                            .createdAt(log.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    public ProductImportResponse importFromXlsx(MultipartFile file, boolean dryRun, boolean upsertBySku) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File xlsx is required", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new BusinessException("Only .xlsx file is supported", HttpStatus.BAD_REQUEST);
        }

        String batchId = "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        List<ProductImportRowResultResponse> results = new ArrayList<>();
        Map<String, Long> categoryLookup = buildCategoryLookupMap();
        int successCount = 0;
        int failedCount = 0;
        int totalRows = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException("Sheet is empty", HttpStatus.BAD_REQUEST);
            }

            DataFormatter formatter = new DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isImportRowEmpty(row, formatter)) {
                    continue;
                }
                totalRows += 1;
                int excelRowNumber = rowIndex + 1;
                String rowName = readCellAsString(row, IMPORT_COL_NAME, formatter);
                try {
                    ProductUpsertRequest request = mapImportRowToRequest(row, formatter, categoryLookup);
                    ProductResponse created;
                    if (upsertBySku) {
                        String sku = request.getVariants().get(0).getSku();
                        ProductVariantEntity existingVariant = productVariantRepository.findBySkuIgnoreCase(sku).orElse(null);
                        if (existingVariant != null) {
                            if (dryRun) {
                                validateUpsertRequest(request);
                                getCategory(request.getCategoryId());
                                resolveSlug(request.getSlug(), request.getName(), existingVariant.getProductId());
                                validateVariantSkus(request.getVariants(), existingVariant.getProductId());
                                ProductEntity existingProduct = productRepository.findById(existingVariant.getProductId())
                                        .orElseThrow(() -> new BusinessException("Product not found for SKU: " + sku, HttpStatus.NOT_FOUND));
                                created = toResponse(existingProduct, false);
                            } else {
                                created = update(existingVariant.getProductId(), request);
                            }
                        } else {
                            if (dryRun) {
                                validateUpsertRequest(request);
                                getCategory(request.getCategoryId());
                                resolveSlug(request.getSlug(), request.getName(), null);
                                validateVariantSkus(request.getVariants(), null);
                                created = ProductResponse.builder().id(null).name(request.getName()).build();
                            } else {
                                created = createInternal(request);
                            }
                        }
                    } else {
                        if (dryRun) {
                            validateUpsertRequest(request);
                            getCategory(request.getCategoryId());
                            resolveSlug(request.getSlug(), request.getName(), null);
                            validateVariantSkus(request.getVariants(), null);
                            created = ProductResponse.builder().id(null).name(request.getName()).build();
                        } else {
                            created = createInternal(request);
                        }
                    }
                    successCount += 1;
                    results.add(ProductImportRowResultResponse.builder()
                            .rowNumber(excelRowNumber)
                            .name(request.getName())
                            .productId(created.getId())
                            .status("SUCCESS")
                            .message(dryRun ? "Validated successfully" : "Imported successfully")
                            .build());
                } catch (Exception ex) {
                    failedCount += 1;
                    String message = ex instanceof BusinessException ? ex.getMessage() : "Import failed";
                    results.add(ProductImportRowResultResponse.builder()
                            .rowNumber(excelRowNumber)
                            .name(rowName)
                            .status("FAILED")
                            .message(message)
                            .build());
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("Cannot read xlsx file", HttpStatus.BAD_REQUEST);
        }

        auditLogService.log(
                "PRODUCT_IMPORT_XLSX",
                "PRODUCT",
                null,
                "batchId=" + batchId +
                        ", dryRun=" + dryRun +
                        ", upsertBySku=" + upsertBySku +
                        ", success=" + successCount +
                        ", failed=" + failedCount
        );

        return ProductImportResponse.builder()
                .batchId(batchId)
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(failedCount)
                .dryRun(dryRun)
                .upsertBySku(upsertBySku)
                .results(results)
                .build();
    }

    @Override
    public byte[] generateImportTemplateXlsx() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("products");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("brand");
            header.createCell(2).setCellValue("description");
            header.createCell(3).setCellValue("category(id/slug/name)");
            header.createCell(4).setCellValue("status");
            header.createCell(5).setCellValue("imageUrl");
            header.createCell(6).setCellValue("sku");
            header.createCell(7).setCellValue("price");
            header.createCell(8).setCellValue("stock");
            header.createCell(9).setCellValue("weight");
            header.createCell(10).setCellValue("size");
            header.createCell(11).setCellValue("color");
            header.createCell(12).setCellValue("slug");

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Giay Chay Bo Alpha");
            sample.createCell(1).setCellValue("TWENTY");
            sample.createCell(2).setCellValue("Giay chay bo nhe, em chan");
            sample.createCell(3).setCellValue("shoes");
            sample.createCell(4).setCellValue("ACTIVE");
            sample.createCell(5).setCellValue("https://example.com/images/alpha.jpg");
            sample.createCell(6).setCellValue("ALPHA-RUN-M-BLK");
            sample.createCell(7).setCellValue(1590000);
            sample.createCell(8).setCellValue(120);
            sample.createCell(9).setCellValue(0.45d);
            sample.createCell(10).setCellValue("M");
            sample.createCell(11).setCellValue("Black");
            sample.createCell(12).setCellValue("giay-chay-bo-alpha");

            for (int i = 0; i <= IMPORT_COL_SLUG; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Cannot generate template xlsx", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ProductUpsertRequest mapImportRowToRequest(Row row, DataFormatter formatter, Map<String, Long> categoryLookup) {
        String name = readRequiredCell(row, IMPORT_COL_NAME, formatter, "name");
        String brand = readCellAsString(row, IMPORT_COL_BRAND, formatter);
        String description = readCellAsString(row, IMPORT_COL_DESCRIPTION, formatter);
        String categoryValue = readRequiredCell(row, IMPORT_COL_CATEGORY, formatter, "category");
        String status = readCellAsString(row, IMPORT_COL_STATUS, formatter);
        String imageUrl = readRequiredCell(row, IMPORT_COL_IMAGE_URL, formatter, "imageUrl");
        String sku = readRequiredCell(row, IMPORT_COL_SKU, formatter, "sku");
        Long price = readRequiredLong(row, IMPORT_COL_PRICE, formatter, "price");
        Integer stock = readRequiredInteger(row, IMPORT_COL_STOCK, formatter, "stock");
        Double weight = readRequiredDouble(row, IMPORT_COL_WEIGHT, formatter, "weight");
        String size = readCellAsString(row, IMPORT_COL_SIZE, formatter);
        String color = readCellAsString(row, IMPORT_COL_COLOR, formatter);
        String slug = readCellAsString(row, IMPORT_COL_SLUG, formatter);

        Long categoryId = resolveCategoryId(categoryValue, categoryLookup);

        ProductVariantRequest variant = new ProductVariantRequest();
        variant.setSku(sku);
        variant.setPrice(price);
        variant.setStock(stock);
        variant.setWeight(weight);
        variant.setStatus(status);
        variant.setSize(size);
        variant.setColor(color);

        ProductImageRequest image = new ProductImageRequest();
        image.setUrl(imageUrl);
        image.setIsMain(true);

        ProductUpsertRequest request = new ProductUpsertRequest();
        request.setName(name);
        request.setSlug(slug);
        request.setBrand(brand);
        request.setDescription(description);
        request.setCategoryId(categoryId);
        request.setStatus(status);
        request.setVariants(List.of(variant));
        request.setImages(List.of(image));
        return request;
    }

    private Map<String, Long> buildCategoryLookupMap() {
        Map<String, Long> lookup = new HashMap<>();
        for (CategoryEntity category : categoryRepository.findAll()) {
            if (category.getId() == null) {
                continue;
            }
            lookup.put(String.valueOf(category.getId()), category.getId());
            if (category.getSlug() != null) {
                lookup.put(category.getSlug().trim().toLowerCase(Locale.ROOT), category.getId());
            }
            if (category.getName() != null) {
                lookup.put(category.getName().trim().toLowerCase(Locale.ROOT), category.getId());
            }
        }
        return lookup;
    }

    private Long resolveCategoryId(String categoryRaw, Map<String, Long> categoryLookup) {
        String normalized = categoryRaw == null ? "" : categoryRaw.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException("category is required", HttpStatus.BAD_REQUEST);
        }
        Long matchedByDirect = categoryLookup.get(normalized);
        if (matchedByDirect != null) {
            return matchedByDirect;
        }
        Long matchedByLower = categoryLookup.get(normalized.toLowerCase(Locale.ROOT));
        if (matchedByLower != null) {
            return matchedByLower;
        }
        throw new BusinessException("Category not found: " + categoryRaw, HttpStatus.BAD_REQUEST);
    }

    private boolean isImportRowEmpty(Row row, DataFormatter formatter) {
        for (int col = IMPORT_COL_NAME; col <= IMPORT_COL_SLUG; col++) {
            if (!readCellAsString(row, col, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCellAsString(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(columnIndex)).trim();
    }

    private String readRequiredCell(Row row, int columnIndex, DataFormatter formatter, String fieldName) {
        String value = readCellAsString(row, columnIndex, formatter);
        if (value.isBlank()) {
            throw new BusinessException(fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private Long readRequiredLong(Row row, int columnIndex, DataFormatter formatter, String fieldName) {
        String raw = readRequiredCell(row, columnIndex, formatter, fieldName);
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(fieldName + " must be a number", HttpStatus.BAD_REQUEST);
        }
    }

    private Integer readRequiredInteger(Row row, int columnIndex, DataFormatter formatter, String fieldName) {
        String raw = readRequiredCell(row, columnIndex, formatter, fieldName);
        try {
            return Integer.parseInt(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(fieldName + " must be an integer", HttpStatus.BAD_REQUEST);
        }
    }

    private Double readRequiredDouble(Row row, int columnIndex, DataFormatter formatter, String fieldName) {
        String raw = readRequiredCell(row, columnIndex, formatter, fieldName);
        try {
            return Double.parseDouble(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(fieldName + " must be a number", HttpStatus.BAD_REQUEST);
        }
    }

    private Set<Long> resolveCategoryTreeIds(Long rootCategoryId) {
        if (rootCategoryId == null) {
            return Set.of();
        }

        Set<Long> allIds = new HashSet<>();
        if (categoryRepository.findById(rootCategoryId).isEmpty()) {
            return allIds;
        }

        List<CategoryEntity> allCategories = categoryRepository.findAll();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootCategoryId);
        allIds.add(rootCategoryId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (CategoryEntity category : allCategories) {
                CategoryEntity parent = category.getParent();
                if (parent == null || parent.getId() == null) {
                    continue;
                }
                if (current.equals(parent.getId()) && allIds.add(category.getId())) {
                    queue.add(category.getId());
                }
            }
        }

        return allIds;
    }

    private String resolveVariantAttributeName(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if ("size".equals(normalized)) {
            return ATTRIBUTE_SIZE;
        }
        if ("color".equals(normalized)) {
            return ATTRIBUTE_COLOR;
        }
        throw new BusinessException("type must be 'size' or 'color'", HttpStatus.BAD_REQUEST);
    }

    private ProductResponse toResponse(ProductEntity product, boolean withRating) {
        String language = resolveLanguage();
        CategoryEntity category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        List<ProductVariantEntity> variants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
        List<ProductImageEntity> images = productImageRepository.findByProductIdOrderByIdAsc(product.getId());
        Double ratingAvg = null;
        Long reviewCount = null;
        if (withRating) {
            Double avg = reviewRepository.findAverageRatingByProductId(product.getId());
            long count = reviewRepository.countByProductId(product.getId());
            ratingAvg = avg == null ? 0d : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
            reviewCount = count;
        }

        String localizedProductName;
        String localizedDescription;
        String localizedCategoryName;
        if ("en".equalsIgnoreCase(language)) {
            localizedProductName = pickFirst(product.getNameEn(), product.getName(), product.getNameVi(), product.getNameMy());
            localizedDescription = pickFirst(product.getDescriptionEn(), product.getDescription(), product.getDescriptionVi(), product.getDescriptionMy());
            localizedCategoryName = category == null
                    ? null
                    : pickFirst(category.getNameEn(), category.getName(), category.getNameVi(), category.getNameMy());
        } else if ("my".equalsIgnoreCase(language)) {
            localizedProductName = pickFirst(product.getNameMy(), product.getName(), product.getNameVi(), product.getNameEn());
            localizedDescription = pickFirst(product.getDescriptionMy(), product.getDescription(), product.getDescriptionVi(), product.getDescriptionEn());
            localizedCategoryName = category == null
                    ? null
                    : pickFirst(category.getNameMy(), category.getName(), category.getNameVi(), category.getNameEn());
        } else {
            localizedProductName = pickFirst(product.getNameVi(), product.getName(), product.getNameEn(), product.getNameMy());
            localizedDescription = pickFirst(product.getDescriptionVi(), product.getDescription(), product.getDescriptionEn(), product.getDescriptionMy());
            localizedCategoryName = category == null
                    ? null
                    : pickFirst(category.getNameVi(), category.getName(), category.getNameEn(), category.getNameMy());
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(localizedProductName)
                .slug(product.getSlug())
                .description(localizedDescription)
                .brand(product.getBrand())
                .categoryId(product.getCategoryId())
                .categoryName(localizedCategoryName)
                .categorySlug(category == null ? null : category.getSlug())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .ratingAvg(ratingAvg)
                .reviewCount(reviewCount)
                .variants(variants.stream().map(this::toVariantResponse).toList())
                .images(images.stream().map(this::toImageResponse).toList())
                .build();
    }

    private String resolveLanguage() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "vi";
        }
        return RequestMetaResolver.resolveLanguage(attributes.getRequest());
    }

    private String localizedOrFallback(String localized, String fallback) {
        if (localized == null || localized.trim().isEmpty()) {
            return fallback;
        }
        return localized;
    }

    private String pickFirst(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeOptionalText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback;
    }

    private ProductVariantResponse toVariantResponse(ProductVariantEntity variant) {
        String size = null;
        String color = null;
        List<VariantAttributeValueEntity> links = variantAttributeValueRepository.findByVariantId(variant.getId());
        for (VariantAttributeValueEntity link : links) {
            AttributeValueEntity value = attributeValueRepository.findById(link.getAttributeValueId()).orElse(null);
            if (value == null) {
                continue;
            }
            AttributeEntity attribute = attributeRepository.findById(value.getAttributeId()).orElse(null);
            if (attribute == null || attribute.getName() == null) {
                continue;
            }
            String attrName = attribute.getName().trim().toLowerCase(Locale.ROOT);
            if ("size".equals(attrName)) {
                size = value.getValue();
            } else if ("color".equals(attrName)) {
                color = value.getValue();
            }
        }

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stock(variant.getStock())
                .weight(variant.getWeight())
                .status(variant.getStatus())
                .size(size)
                .color(color)
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImageEntity image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isMain(Boolean.TRUE.equals(image.getIsMain()))
                .build();
    }

    private void saveImages(Long productId, List<ProductImageRequest> images) {
        boolean hasMain = images.stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsMain()));
        for (int i = 0; i < images.size(); i++) {
            ProductImageRequest request = images.get(i);
            ProductImageEntity image = new ProductImageEntity();
            image.setProductId(productId);
            image.setUrl(request.getUrl().trim());
            image.setIsMain(hasMain ? Boolean.TRUE.equals(request.getIsMain()) : i == 0);
            productImageRepository.save(image);
        }
    }

    private void saveVariants(Long productId, List<ProductVariantRequest> variants) {
        AttributeEntity sizeAttribute = getOrCreateAttribute(ATTRIBUTE_SIZE);
        AttributeEntity colorAttribute = getOrCreateAttribute(ATTRIBUTE_COLOR);

        for (ProductVariantRequest request : variants) {
            ProductVariantEntity variant = new ProductVariantEntity();
            variant.setProductId(productId);
            variant.setSku(request.getSku().trim());
            variant.setPrice(request.getPrice());
            variant.setStock(0);
            variant.setWeight(request.getWeight());
            variant.setStatus(normalizeStatus(request.getStatus()));
            ProductVariantEntity savedVariant = productVariantRepository.save(variant);
            inventoryMovementService.setAbsoluteStockByVariantId(
                    savedVariant.getId(),
                    request.getStock(),
                    "ADJUST",
                    "Initial stock from product create"
            );

            if (request.getSize() != null && !request.getSize().isBlank()) {
                AttributeValueEntity sizeValue = getOrCreateAttributeValue(sizeAttribute.getId(), request.getSize().trim());
                linkVariantWithAttributeValue(savedVariant.getId(), sizeValue.getId());
            }
            if (request.getColor() != null && !request.getColor().isBlank()) {
                AttributeValueEntity colorValue = getOrCreateAttributeValue(colorAttribute.getId(), request.getColor().trim());
                linkVariantWithAttributeValue(savedVariant.getId(), colorValue.getId());
            }
        }
    }

    private void upsertVariants(Long productId, List<ProductVariantRequest> variants) {
        AttributeEntity sizeAttribute = getOrCreateAttribute(ATTRIBUTE_SIZE);
        AttributeEntity colorAttribute = getOrCreateAttribute(ATTRIBUTE_COLOR);
        List<ProductVariantEntity> existingVariants = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        Map<Long, ProductVariantEntity> existingById = new HashMap<>();
        Map<String, ProductVariantEntity> existingBySku = new HashMap<>();
        for (ProductVariantEntity existing : existingVariants) {
            existingById.put(existing.getId(), existing);
            if (existing.getSku() != null) {
                existingBySku.put(existing.getSku().trim().toLowerCase(Locale.ROOT), existing);
            }
        }

        Set<Long> handledVariantIds = new HashSet<>();

        for (ProductVariantRequest request : variants) {
            ProductVariantEntity target = resolveVariantForUpsert(request, existingById, existingBySku);
            if (target.getId() != null && !handledVariantIds.add(target.getId())) {
                throw new BusinessException("Duplicate variant in request: " + target.getId(), HttpStatus.BAD_REQUEST);
            }
            boolean isNew = target.getId() == null;
            target.setProductId(productId);
            target.setSku(request.getSku().trim());
            target.setPrice(request.getPrice());
            target.setWeight(request.getWeight());
            target.setStatus(normalizeStatus(request.getStatus()));
            if (isNew) {
                target.setStock(0);
            }

            ProductVariantEntity saved = productVariantRepository.save(target);
            handledVariantIds.add(saved.getId());
            syncVariantAttributes(saved.getId(), request, sizeAttribute, colorAttribute);
            inventoryMovementService.setAbsoluteStockByVariantId(
                    saved.getId(),
                    request.getStock(),
                    "ADJUST",
                    "Adjusted stock from product update"
            );
        }

        for (ProductVariantEntity existing : existingVariants) {
            if (handledVariantIds.contains(existing.getId())) {
                continue;
            }
            existing.setStatus("INACTIVE");
            productVariantRepository.save(existing);
        }
    }

    private Long findCurrentMinPrice(Long productId) {
        return productVariantRepository.findByProductIdOrderByIdAsc(productId).stream()
                .map(ProductVariantEntity::getPrice)
                .filter(price -> price != null && price >= 0L)
                .min(Long::compareTo)
                .orElse(0L);
    }

    private ProductVariantEntity resolveVariantForUpsert(
            ProductVariantRequest request,
            Map<Long, ProductVariantEntity> existingById,
            Map<String, ProductVariantEntity> existingBySku
    ) {
        if (request.getId() != null) {
            ProductVariantEntity byId = existingById.get(request.getId());
            if (byId == null) {
                throw new BusinessException("Variant not found in product: " + request.getId(), HttpStatus.BAD_REQUEST);
            }
            return byId;
        }

        String skuKey = request.getSku().trim().toLowerCase(Locale.ROOT);
        return existingBySku.getOrDefault(skuKey, new ProductVariantEntity());
    }

    private void syncVariantAttributes(
            Long variantId,
            ProductVariantRequest request,
            AttributeEntity sizeAttribute,
            AttributeEntity colorAttribute
    ) {
        variantAttributeValueRepository.deleteByVariantId(variantId);

        if (request.getSize() != null && !request.getSize().isBlank()) {
            AttributeValueEntity sizeValue = getOrCreateAttributeValue(sizeAttribute.getId(), request.getSize().trim());
            linkVariantWithAttributeValue(variantId, sizeValue.getId());
        }
        if (request.getColor() != null && !request.getColor().isBlank()) {
            AttributeValueEntity colorValue = getOrCreateAttributeValue(colorAttribute.getId(), request.getColor().trim());
            linkVariantWithAttributeValue(variantId, colorValue.getId());
        }
    }

    private void linkVariantWithAttributeValue(Long variantId, Long attributeValueId) {
        VariantAttributeValueEntity link = new VariantAttributeValueEntity();
        link.setVariantId(variantId);
        link.setAttributeValueId(attributeValueId);
        variantAttributeValueRepository.save(link);
    }

    private AttributeEntity getOrCreateAttribute(String name) {
        return attributeRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    AttributeEntity entity = new AttributeEntity();
                    entity.setName(name);
                    return attributeRepository.save(entity);
                });
    }

    private AttributeValueEntity getOrCreateAttributeValue(Long attributeId, String value) {
        return attributeValueRepository.findByAttributeIdAndValueIgnoreCase(attributeId, value)
                .orElseGet(() -> {
                    AttributeValueEntity entity = new AttributeValueEntity();
                    entity.setAttributeId(attributeId);
                    entity.setValue(value);
                    return attributeValueRepository.save(entity);
                });
    }

    private CategoryEntity getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found", HttpStatus.BAD_REQUEST));
    }

    private ProductEntity findActiveProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException("Product not found", HttpStatus.NOT_FOUND);
        }
        return product;
    }

    private String resolveSlug(String rawSlug, String name, Long currentId) {
        String slug = rawSlug == null || rawSlug.isBlank() ? toSlug(name) : toSlug(rawSlug);
        if (slug.isBlank()) {
            throw new BusinessException("Slug is invalid", HttpStatus.BAD_REQUEST);
        }

        boolean exists = productRepository.existsBySlugIgnoreCase(slug);
        if (!exists) {
            return slug;
        }
        if (currentId == null) {
            throw new BusinessException("Product slug already exists", HttpStatus.CONFLICT);
        }
        ProductEntity existing = productRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new BusinessException("Product slug already exists", HttpStatus.CONFLICT));
        if (!existing.getId().equals(currentId)) {
            throw new BusinessException("Product slug already exists", HttpStatus.CONFLICT);
        }
        return slug;
    }

    private void validateVariantSkus(List<ProductVariantRequest> variants, Long currentProductId) {
        Set<String> requestSkus = new HashSet<>();
        Set<Long> requestIds = new HashSet<>();
        for (ProductVariantRequest variant : variants) {
            String sku = variant.getSku().trim().toLowerCase(Locale.ROOT);
            if (!requestSkus.add(sku)) {
                throw new BusinessException("Duplicate SKU in request: " + variant.getSku(), HttpStatus.BAD_REQUEST);
            }
            if (variant.getId() != null && !requestIds.add(variant.getId())) {
                throw new BusinessException("Duplicate variant id in request: " + variant.getId(), HttpStatus.BAD_REQUEST);
            }
        }

        if (currentProductId == null) {
            for (ProductVariantRequest variant : variants) {
                if (productVariantRepository.existsBySkuIgnoreCase(variant.getSku().trim())) {
                    throw new BusinessException("SKU already exists: " + variant.getSku(), HttpStatus.CONFLICT);
                }
            }
            return;
        }

        List<ProductVariantEntity> currentVariants = productVariantRepository.findByProductIdOrderByIdAsc(currentProductId);
        Map<String, Long> currentSkuMap = new HashMap<>();
        Set<Long> currentVariantIds = new HashSet<>();
        for (ProductVariantEntity entity : currentVariants) {
            currentSkuMap.put(entity.getSku().toLowerCase(Locale.ROOT), entity.getId());
            currentVariantIds.add(entity.getId());
        }

        for (ProductVariantRequest variant : variants) {
            if (variant.getId() != null && !currentVariantIds.contains(variant.getId())) {
                throw new BusinessException("Variant not found in product: " + variant.getId(), HttpStatus.BAD_REQUEST);
            }
            String sku = variant.getSku().trim();
            ProductVariantEntity existing = productVariantRepository.findBySkuIgnoreCase(sku).orElse(null);
            if (existing == null) {
                continue;
            }
            if (currentProductId.equals(existing.getProductId())
                    && variant.getId() != null
                    && !existing.getId().equals(variant.getId())) {
                throw new BusinessException("SKU already exists: " + sku, HttpStatus.CONFLICT);
            }
            if (!currentProductId.equals(existing.getProductId())
                    && !currentSkuMap.containsKey(sku.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("SKU already exists: " + sku, HttpStatus.CONFLICT);
            }
        }
    }

    private String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? STATUS_ACTIVE : status.trim().toUpperCase(Locale.ROOT);
    }

    private void validateUpsertRequest(ProductUpsertRequest request) {
        if (request.getName() == null || request.getName().trim().length() < 3) {
            throw new BusinessException("name must be at least 3 characters", HttpStatus.BAD_REQUEST);
        }
        if (request.getSlug() != null && !request.getSlug().isBlank() && request.getSlug().trim().length() < 3) {
            throw new BusinessException("slug must be at least 3 characters", HttpStatus.BAD_REQUEST);
        }
        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new BusinessException("variants must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (request.getImages() == null || request.getImages().isEmpty()) {
            throw new BusinessException("images must not be empty", HttpStatus.BAD_REQUEST);
        }
        boolean hasMainImage = request.getImages()
                .stream()
                .anyMatch(image -> image != null && Boolean.TRUE.equals(image.getIsMain()));
        if (!hasMainImage && !request.getImages().isEmpty()) {
            request.getImages().get(0).setIsMain(true);
        }
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim()
                .replace('đ', 'd')
                .replace('Đ', 'D');

        String noAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noAccent
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }
}
