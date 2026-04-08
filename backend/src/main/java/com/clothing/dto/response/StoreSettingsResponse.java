package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreSettingsResponse {

    private String storeName;
    private String hotline;
    private String supportEmail;
    private String address;
    private Long defaultShippingFee;
    private Long freeShippingThreshold;
    private Boolean enableCOD;
    private Boolean enableMomo;
    private String shippingPolicy;
    private String returnPolicy;
}
