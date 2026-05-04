package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productSlug;
    private String productName;
    private String productImage;
    private Long variantId;
    private String sku;
    private String size;
    private String color;
    private Long price;
    private Integer quantity;
    private Long lineTotal;
}
