package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotBlank(message = "status is required")
    @Size(max = 50, message = "status max length is 50")
    private String status;

    private Boolean syncWithGhn;

    @Size(max = 100, message = "shippingCode max length is 100")
    private String shippingCode;
}
