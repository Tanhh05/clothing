package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InventoryAlertResponse {

    private Long variantId;
    private String sku;
    private Integer stock;
    private Long productId;
    private String productName;
}

