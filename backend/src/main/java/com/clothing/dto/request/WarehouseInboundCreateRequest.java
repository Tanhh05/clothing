package com.clothing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WarehouseInboundCreateRequest {

    @NotBlank(message = "code is required")
    @Size(max = 100, message = "code max length is 100")
    private String code;

    @NotBlank(message = "supplier is required")
    @Size(max = 255, message = "supplier max length is 255")
    private String supplier;

    @NotNull(message = "createdAt is required")
    private LocalDateTime createdAt;

    @NotEmpty(message = "items is required")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    public static class ItemRequest {

        @NotBlank(message = "sku is required")
        @Size(max = 100, message = "sku max length is 100")
        private String sku;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be >= 1")
        private Integer quantity;

        @NotNull(message = "cost is required")
        @Min(value = 0, message = "cost must be >= 0")
        private Long cost;
    }
}
