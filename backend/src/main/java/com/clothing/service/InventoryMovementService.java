package com.clothing.service;

import com.clothing.entity.ProductVariantEntity;

public interface InventoryMovementService {

    ProductVariantEntity increaseStockBySku(String sku, int quantity, String type, String note);

    ProductVariantEntity deductStockByVariantId(Long variantId, int quantity, String type, String note);

    ProductVariantEntity setAbsoluteStockByVariantId(Long variantId, int targetStock, String type, String note);
}
