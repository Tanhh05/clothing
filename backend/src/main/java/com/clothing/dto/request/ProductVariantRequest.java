package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantRequest {

    private Long id;

    @NotBlank(message = "sku is required")
    @Size(max = 100, message = "sku max length is 100")
    private String sku;

    @NotNull(message = "price is required")
    @PositiveOrZero(message = "price must be >= 0")
    private Long price;

    @NotNull(message = "stock is required")
    @PositiveOrZero(message = "stock must be >= 0")
    private Integer stock;

    @NotNull(message = "weight is required")
    @PositiveOrZero(message = "weight must be >= 0")
    private Double weight;

    @Size(max = 20, message = "status max length is 20")
    private String status;

    @Size(max = 50, message = "size max length is 50")
    private String size;

    @Size(max = 50, message = "color max length is 50")
    private String color;
}
