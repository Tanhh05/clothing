package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VoucherResponse {

    private Long id;
    private String code;
    private String discountType;
    private Long discountValue;
    private Long minOrderValue;
    private Integer maxUsage;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private LocalDateTime createdAt;
}
