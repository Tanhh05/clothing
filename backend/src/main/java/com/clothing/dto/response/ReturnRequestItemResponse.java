package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnRequestItemResponse {

    private Long id;
    private Long orderItemId;
    private Long variantId;
    private String sku;
    private String productName;
    private Integer requestedQuantity;
}
