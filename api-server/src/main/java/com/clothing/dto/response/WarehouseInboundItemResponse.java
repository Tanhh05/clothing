package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WarehouseInboundItemResponse {

    private Long id;
    private String sku;
    private Integer quantity;
    private Long unitCost;
    private Long lineTotal;
    private Integer currentStock;
}
