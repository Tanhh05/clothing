package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String customerName;
    private Long totalPrice;
    private String status;
    private String paymentMethod;
    private String paymentUrl;
    private String address;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private List<OrderStatusHistoryResponse> statusHistory;
}
