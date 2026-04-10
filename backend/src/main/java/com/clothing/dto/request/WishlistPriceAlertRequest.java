package com.clothing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishlistPriceAlertRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "targetPrice is required")
    @Min(value = 0, message = "targetPrice must be >= 0")
    private Long targetPrice;
}
