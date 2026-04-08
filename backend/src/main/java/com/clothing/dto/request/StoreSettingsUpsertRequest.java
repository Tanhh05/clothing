package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreSettingsUpsertRequest {

    @NotBlank(message = "storeName is required")
    private String storeName;

    @NotBlank(message = "hotline is required")
    private String hotline;

    @NotBlank(message = "supportEmail is required")
    private String supportEmail;

    @NotBlank(message = "address is required")
    private String address;

    @NotNull(message = "defaultShippingFee is required")
    private Long defaultShippingFee;

    @NotNull(message = "freeShippingThreshold is required")
    private Long freeShippingThreshold;

    @NotNull(message = "enableCOD is required")
    private Boolean enableCOD;

    @NotNull(message = "enableMomo is required")
    private Boolean enableMomo;

    @NotBlank(message = "shippingPolicy is required")
    private String shippingPolicy;

    @NotBlank(message = "returnPolicy is required")
    private String returnPolicy;
}
