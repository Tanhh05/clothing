package com.clothing.service.impl;

import com.clothing.entity.InventoryLogEntity;
import com.clothing.entity.NotificationEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.StockAlertSubscriptionEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.repository.NotificationRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.StockAlertSubscriptionRepository;
import com.clothing.service.InventoryMovementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryMovementServiceImpl implements InventoryMovementService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final StockAlertSubscriptionRepository stockAlertSubscriptionRepository;
    private final NotificationRepository notificationRepository;

    public InventoryMovementServiceImpl(
            ProductVariantRepository productVariantRepository,
            InventoryLogRepository inventoryLogRepository,
            StockAlertSubscriptionRepository stockAlertSubscriptionRepository,
            NotificationRepository notificationRepository
    ) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.stockAlertSubscriptionRepository = stockAlertSubscriptionRepository;
        this.notificationRepository = notificationRepository;
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
        if (before <= 0 && after > 0) {
            notifyBackInStock(variant.getProductId(), variant.getSku());
        }
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

    private void notifyBackInStock(Long productId, String sku) {
        List<StockAlertSubscriptionEntity> subscriptions = stockAlertSubscriptionRepository.findByProductIdAndNotifiedFalse(productId);
        if (subscriptions.isEmpty()) return;
        for (StockAlertSubscriptionEntity subscription : subscriptions) {
            NotificationEntity notification = new NotificationEntity();
            notification.setUserId(subscription.getUserId());
            notification.setTitle("Sản phẩm đã có hàng lại");
            notification.setContent("Sản phẩm bạn theo dõi đã có hàng lại. SKU: " + sku);
            notification.setType("BACK_IN_STOCK");
            notification.setAudience("USER");
            notification.setChannel("IN_APP");
            notification.setStatus("SENT");
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            subscription.setNotified(true);
        }
        stockAlertSubscriptionRepository.saveAll(subscriptions);
    }
}
