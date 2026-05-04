package com.clothing.service;

public interface GhnShippingService {

    boolean canCallShippingApi();

    GhnOrderDetail getOrderDetail(String orderCode);

    String createShippingOrder(CreateShippingOrderRequest request);

    String toInternalOrderStatus(String ghnStatus);

    String normalizeGhnStatus(String ghnStatus);

    record CreateShippingOrderRequest(
            String clientOrderCode,
            String toName,
            String toPhone,
            String toAddress,
            String toProvinceName,
            String toDistrictName,
            String toWardName,
            Long codAmount,
            Long totalWeightGrams
    ) {}

    record GhnOrderDetail(String orderCode, String status) {}
}
