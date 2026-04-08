package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WarehouseInboundResponse {

    private Long id;
    private String code;
    private String supplier;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private Long totalCost;
}
