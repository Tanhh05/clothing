package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoucherBestResponse {

    private String code;
    private Long discountAmount;
    private Long finalTotal;
    private Boolean autoApplied;
}
