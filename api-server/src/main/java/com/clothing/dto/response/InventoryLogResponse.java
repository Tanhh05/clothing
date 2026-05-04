package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class InventoryLogResponse {

    private Long id;
    private Long variantId;
    private String sku;
    private String type;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private String note;
    private LocalDateTime createdAt;
}

