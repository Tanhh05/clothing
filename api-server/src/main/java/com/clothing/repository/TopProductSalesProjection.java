package com.clothing.repository;

public interface TopProductSalesProjection {

    Long getProductId();

    String getProductName();

    Long getTotalQuantity();

    Long getTotalRevenue();
}
