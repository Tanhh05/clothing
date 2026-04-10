package com.clothing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAlertSubscribeRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @Size(max = 50, message = "size max length is 50")
    private String size;

    @Size(max = 80, message = "color max length is 80")
    private String color;
}
