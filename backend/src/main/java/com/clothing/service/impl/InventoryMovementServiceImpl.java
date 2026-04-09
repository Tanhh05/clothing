package com.clothing.service.impl;

import com.clothing.entity.InventoryLogEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.service.InventoryMovementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryMovementServiceImpl implements InventoryMovementService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public InventoryMovementServiceImpl(
            ProductVariantRepository productVariantRepository,
            InventoryLogRepository inventoryLogRepository
    ) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @Override
    public ProductVariantEntity increaseStockBySku(String sku, int quantity, String type, String note) {
        if (quantity <= 0) {
            throw new BusinessException("quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        ProductVariantEntity variant = productVariantRepository.findBySkuIgnoreCaseForUpdate(sku)
                .orElseThrow(() -> new BusinessException("SKU not found: " + sku, HttpStatus.BAD_REQUEST));
        int before = variant.getStock() == null ? 0 : variant.getStock();
        int after = before + quantity;
        variant.setStock(after);
        productVariantRepository.save(variant);
        saveLog(variant.getId(), type, quantity, before, after, note);
        return variant;
    }

    @Override
    public ProductVariantEntity deductStockByVariantId(Long variantId, int quantity, String type, String note) {
        if (quantity <= 0) {
            throw new BusinessException("quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        ProductVariantEntity variant = productVariantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new BusinessException("Variant not found: " + variantId, HttpStatus.BAD_REQUEST));
        int before = variant.getStock() == null ? 0 : variant.getStock();
        if (before < quantity) {
            throw new BusinessException("Not enough stock for SKU: " + variant.getSku(), HttpStatus.BAD_REQUEST);
        }
        int after = before - quantity;
        variant.setStock(after);
        productVariantRepository.save(variant);
        saveLog(variant.getId(), type, quantity, before, after, note);
        return variant;
    }

    @Override
    public ProductVariantEntity setAbsoluteStockByVariantId(Long variantId, int targetStock, String type, String note) {
        if (targetStock < 0) {
            throw new BusinessException("stock must be >= 0", HttpStatus.BAD_REQUEST);
        }
        ProductVariantEntity variant = productVariantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new BusinessException("Variant not found: " + variantId, HttpStatus.BAD_REQUEST));
        int before = variant.getStock() == null ? 0 : variant.getStock();
        if (before == targetStock) {
            return variant;
        }
        variant.setStock(targetStock);
        productVariantRepository.save(variant);
        int quantity = Math.abs(targetStock - before);
        saveLog(variant.getId(), type, quantity, before, targetStock, note);
        return variant;
    }

    private void saveLog(Long variantId, String type, int quantity, int before, int after, String note) {
        InventoryLogEntity log = new InventoryLogEntity();
        log.setVariantId(variantId);
        log.setType(type == null || type.isBlank() ? "ADJUST" : type.trim().toUpperCase());
        log.setQuantity(quantity);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setNote(note);
        log.setCreatedAt(LocalDateTime.now());
        inventoryLogRepository.save(log);
    }
}
