package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WarehouseInboundDetailResponse {

    private Long id;
    private String code;
    private String supplier;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private Long totalQuantity;
    private Long totalCost;
    private List<WarehouseInboundItemResponse> items;
}
