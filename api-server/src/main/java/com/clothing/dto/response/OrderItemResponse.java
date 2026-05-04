package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Long variantId;
    private String sku;
    private String productName;
    private Integer quantity;
    private Long price;
    private Long lineTotal;
}
