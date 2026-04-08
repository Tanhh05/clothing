package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    private Long id;
    private Long variantId;
    private String sku;
    private Long price;
    private Integer quantity;
    private Long lineTotal;
}
