package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 50, message = "paymentMethod max length is 50")
    private String paymentMethod;

    @NotBlank(message = "address is required")
    private String address;

    @Size(max = 100, message = "province max length is 100")
    private String province;
}
