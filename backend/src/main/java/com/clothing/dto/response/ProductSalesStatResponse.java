package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductSalesStatResponse {

    private Long productId;
    private String productName;
    private Long totalQuantity;
    private Long totalRevenue;
}

