package com.clothing.service.impl;

import com.clothing.dto.request.WarehouseInboundCreateRequest;
import com.clothing.dto.response.WarehouseInboundResponse;
import com.clothing.entity.InventoryLogEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.WarehouseInboundEntity;
import com.clothing.entity.WarehouseInboundItemEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.WarehouseInboundItemRepository;
import com.clothing.repository.WarehouseInboundRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.WarehouseInboundService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        return warehouseInboundRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public WarehouseInboundResponse create(WarehouseInboundCreateRequest request) {
        if (warehouseInboundRepository.findByCodeIgnoreCase(request.getCode().trim()).isPresent()) {
            throw new BusinessException("Inbound code already exists", HttpStatus.BAD_REQUEST);
        }

        WarehouseInboundEntity inbound = new WarehouseInboundEntity();
        inbound.setCode(request.getCode().trim().toUpperCase());
        inbound.setSupplier(request.getSupplier().trim());
        inbound.setInboundAt(request.getCreatedAt());
        inbound.setCreatedAt(LocalDateTime.now());

        long totalCost = 0L;
        int itemCount = 0;
        for (WarehouseInboundCreateRequest.ItemRequest item : request.getItems()) {
            long lineTotal = item.getCost() * item.getQuantity();
            totalCost += lineTotal;
            itemCount += 1;
        }

        inbound.setItemCount(itemCount);
        inbound.setTotalCost(totalCost);
        WarehouseInboundEntity savedInbound = warehouseInboundRepository.save(inbound);

        for (WarehouseInboundCreateRequest.ItemRequest item : request.getItems()) {
            ProductVariantEntity variant = productVariantRepository.findBySkuIgnoreCase(item.getSku().trim())
                    .orElseThrow(() -> new BusinessException("SKU not found: " + item.getSku(), HttpStatus.BAD_REQUEST));

            WarehouseInboundItemEntity detail = new WarehouseInboundItemEntity();
            detail.setInboundId(savedInbound.getId());
            detail.setSku(variant.getSku());
            detail.setQuantity(item.getQuantity());
            detail.setUnitCost(item.getCost());
            detail.setLineTotal(item.getCost() * item.getQuantity());
            warehouseInboundItemRepository.save(detail);

            int beforeStock = variant.getStock() == null ? 0 : variant.getStock();
            int afterStock = beforeStock + item.getQuantity();
            variant.setStock(afterStock);
            productVariantRepository.save(variant);

            InventoryLogEntity log = new InventoryLogEntity();
            log.setVariantId(variant.getId());
            log.setType("IN");
            log.setQuantity(item.getQuantity());
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

        return toResponse(savedInbound);
    }

    private WarehouseInboundResponse toResponse(WarehouseInboundEntity entity) {
        return WarehouseInboundResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .supplier(entity.getSupplier())
                .createdAt(entity.getInboundAt())
                .itemCount(entity.getItemCount())
                .totalCost(entity.getTotalCost())
                .build();
    }
}
