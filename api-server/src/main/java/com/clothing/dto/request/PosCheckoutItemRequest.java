package com.clothing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosCheckoutItemRequest {

    @NotNull(message = "variantId is required")
    private Long variantId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;
}
