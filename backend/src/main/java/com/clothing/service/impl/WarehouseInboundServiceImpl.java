package com.clothing.service.impl;

import com.clothing.dto.request.WarehouseInboundCreateRequest;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.WarehouseInboundDetailResponse;
import com.clothing.dto.response.WarehouseInboundItemResponse;
import com.clothing.dto.response.WarehouseInboundResponse;
import com.clothing.entity.InventoryLogEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.WarehouseInboundEntity;
import com.clothing.entity.WarehouseInboundItemEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.InboundQuantityProjection;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.WarehouseInboundItemRepository;
import com.clothing.repository.WarehouseInboundRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.WarehouseInboundService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WarehouseInboundServiceImpl implements WarehouseInboundService {

    private final WarehouseInboundRepository warehouseInboundRepository;
    private final WarehouseInboundItemRepository warehouseInboundItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final AuditLogService auditLogService;

    public WarehouseInboundServiceImpl(
            WarehouseInboundRepository warehouseInboundRepository,
            WarehouseInboundItemRepository warehouseInboundItemRepository,
            ProductVariantRepository productVariantRepository,
            InventoryLogRepository inventoryLogRepository,
            AuditLogService auditLogService
    ) {
        this.warehouseInboundRepository = warehouseInboundRepository;
        this.warehouseInboundItemRepository = warehouseInboundItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<WarehouseInboundResponse> getAll() {
        List<WarehouseInboundEntity> entities = warehouseInboundRepository.findAllByOrderByIdDesc();
        Map<Long, Long> quantityMap = resolveQuantityMap(entities.stream().map(WarehouseInboundEntity::getId).toList());
        return entities.stream()
                .map(entity -> toResponse(entity, quantityMap.getOrDefault(entity.getId(), 0L)))
                .toList();
    }

    @Override
    public PageResponse<WarehouseInboundResponse> getPage(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            LocalDateTime from,
            LocalDateTime to
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String safeSortBy = resolveSortBy(sortBy);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(sortDirection, safeSortBy));

        Specification<WarehouseInboundEntity> specification = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), keyword),
                    cb.like(cb.lower(root.get("supplier")), keyword)
            ));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("inboundAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("inboundAt"), to));
        }

        Page<WarehouseInboundEntity> entityPage = warehouseInboundRepository.findAll(specification, pageable);
        List<WarehouseInboundEntity> content = entityPage.getContent();
        Map<Long, Long> quantityMap = resolveQuantityMap(content.stream().map(WarehouseInboundEntity::getId).toList());

        List<WarehouseInboundResponse> responses = content.stream()
                .map(entity -> toResponse(entity, quantityMap.getOrDefault(entity.getId(), 0L)))
                .toList();

        return PageResponse.<WarehouseInboundResponse>builder()
                .content(responses)
                .page(entityPage.getNumber())
                .size(entityPage.getSize())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .first(entityPage.isFirst())
                .last(entityPage.isLast())
                .build();
    }

    @Override
    public WarehouseInboundDetailResponse getById(Long id) {
        WarehouseInboundEntity inbound = warehouseInboundRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inbound not found", HttpStatus.NOT_FOUND));

        List<WarehouseInboundItemEntity> itemEntities = warehouseInboundItemRepository.findByInboundIdOrderByIdAsc(id);
        List<String> skus = itemEntities.stream()
                .map(WarehouseInboundItemEntity::getSku)
                .filter(sku -> sku != null && !sku.isBlank())
                .distinct()
                .toList();

        Map<String, Integer> stockBySku = productVariantRepository.findBySkuIn(skus).stream()
                .collect(Collectors.toMap(
                        ProductVariantEntity::getSku,
                        variant -> variant.getStock() == null ? 0 : variant.getStock(),
                        (v1, v2) -> v1
                ));

        long totalQuantity = 0L;
        List<WarehouseInboundItemResponse> items = new ArrayList<>();
        for (WarehouseInboundItemEntity itemEntity : itemEntities) {
            int quantity = itemEntity.getQuantity() == null ? 0 : itemEntity.getQuantity();
            totalQuantity += quantity;
            items.add(WarehouseInboundItemResponse.builder()
                    .id(itemEntity.getId())
                    .sku(itemEntity.getSku())
                    .quantity(quantity)
                    .unitCost(itemEntity.getUnitCost())
                    .lineTotal(itemEntity.getLineTotal())
                    .currentStock(stockBySku.getOrDefault(itemEntity.getSku(), 0))
                    .build());
        }

        return WarehouseInboundDetailResponse.builder()
                .id(inbound.getId())
                .code(inbound.getCode())
                .supplier(inbound.getSupplier())
                .createdAt(inbound.getInboundAt())
                .itemCount(inbound.getItemCount())
                .totalQuantity(totalQuantity)
                .totalCost(inbound.getTotalCost())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public WarehouseInboundResponse create(WarehouseInboundCreateRequest request) {
        String code = normalizeUpper(request.getCode());
        if (warehouseInboundRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessException("Inbound code already exists", HttpStatus.BAD_REQUEST);
        }

        List<NormalizedInboundItem> normalizedItems = normalizeItems(request.getItems());

        WarehouseInboundEntity inbound = new WarehouseInboundEntity();
        inbound.setCode(code);
        inbound.setSupplier(normalizeText(request.getSupplier()));
        inbound.setInboundAt(request.getCreatedAt());
        inbound.setCreatedAt(LocalDateTime.now());

        long totalCost = 0L;
        long totalQuantity = 0L;
        for (NormalizedInboundItem item : normalizedItems) {
            long lineTotal = item.lineTotal;
            totalCost += lineTotal;
            totalQuantity += item.quantity;
        }

        inbound.setItemCount(normalizedItems.size());
        inbound.setTotalCost(totalCost);
        WarehouseInboundEntity savedInbound = warehouseInboundRepository.save(inbound);

        for (NormalizedInboundItem item : normalizedItems) {
            ProductVariantEntity variant = productVariantRepository.findBySkuIgnoreCaseForUpdate(item.sku)
                    .orElseThrow(() -> new BusinessException("SKU not found: " + item.getSku(), HttpStatus.BAD_REQUEST));

            WarehouseInboundItemEntity detail = new WarehouseInboundItemEntity();
            detail.setInboundId(savedInbound.getId());
            detail.setSku(item.sku);
            detail.setQuantity(item.quantity);
            detail.setUnitCost(item.cost);
            detail.setLineTotal(item.lineTotal);
            warehouseInboundItemRepository.save(detail);

            int beforeStock = variant.getStock() == null ? 0 : variant.getStock();
            int afterStock = beforeStock + item.quantity;
            variant.setStock(afterStock);
            productVariantRepository.save(variant);

            InventoryLogEntity log = new InventoryLogEntity();
            log.setVariantId(variant.getId());
            log.setType("IN");
            log.setQuantity(item.quantity);
            log.setBeforeStock(beforeStock);
            log.setAfterStock(afterStock);
            log.setNote("Inbound " + savedInbound.getCode() + " - " + savedInbound.getSupplier());
            log.setCreatedAt(LocalDateTime.now());
            inventoryLogRepository.save(log);
        }

        auditLogService.log(
                "WAREHOUSE_INBOUND_CREATED",
                "WAREHOUSE_INBOUND",
                savedInbound.getId(),
                "Created inbound receipt " + savedInbound.getCode()
        );

        return toResponse(savedInbound, totalQuantity);
    }

    private Map<Long, Long> resolveQuantityMap(List<Long> inboundIds) {
        if (inboundIds == null || inboundIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> quantityMap = new HashMap<>();
        for (InboundQuantityProjection projection : warehouseInboundItemRepository.sumQuantityByInboundIds(inboundIds)) {
            quantityMap.put(projection.getInboundId(), projection.getTotalQuantity());
        }
        return quantityMap;
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return switch (sortBy) {
            case "id", "code", "supplier", "inboundAt", "itemCount", "totalCost", "createdAt" -> sortBy;
            default -> "id";
        };
    }

    private List<NormalizedInboundItem> normalizeItems(List<WarehouseInboundCreateRequest.ItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BusinessException("At least 1 item is required", HttpStatus.BAD_REQUEST);
        }
        Set<String> seenSkus = new HashSet<>();
        List<NormalizedInboundItem> normalizedItems = new ArrayList<>();
        for (WarehouseInboundCreateRequest.ItemRequest itemRequest : itemRequests) {
            String sku = normalizeUpper(itemRequest.getSku());
            if (!seenSkus.add(sku)) {
                throw new BusinessException("Duplicate SKU in receipt: " + sku, HttpStatus.BAD_REQUEST);
            }
            int quantity = itemRequest.getQuantity() == null ? 0 : itemRequest.getQuantity();
            long cost = itemRequest.getCost() == null ? 0 : itemRequest.getCost();
            if (quantity <= 0) {
                throw new BusinessException("Quantity must be greater than 0: " + sku, HttpStatus.BAD_REQUEST);
            }
            if (cost < 0) {
                throw new BusinessException("Cost must be greater than or equal to 0: " + sku, HttpStatus.BAD_REQUEST);
            }
            long lineTotal;
            try {
                lineTotal = Math.multiplyExact(cost, quantity);
            } catch (ArithmeticException ex) {
                throw new BusinessException("Line total is too large for SKU: " + sku, HttpStatus.BAD_REQUEST);
            }
            normalizedItems.add(new NormalizedInboundItem(sku, quantity, cost, lineTotal));
        }
        return normalizedItems;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeUpper(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private WarehouseInboundResponse toResponse(WarehouseInboundEntity entity, Long totalQuantity) {
        return WarehouseInboundResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .supplier(entity.getSupplier())
                .createdAt(entity.getInboundAt())
                .itemCount(entity.getItemCount())
                .totalQuantity(totalQuantity == null ? 0L : totalQuantity)
                .totalCost(entity.getTotalCost())
                .build();
    }

    private static class NormalizedInboundItem {
        private final String sku;
        private final int quantity;
        private final long cost;
        private final long lineTotal;

        private NormalizedInboundItem(String sku, int quantity, long cost, long lineTotal) {
            this.sku = sku;
            this.quantity = quantity;
            this.cost = cost;
            this.lineTotal = lineTotal;
        }

        private String getSku() {
            return sku;
        }
    }
}
