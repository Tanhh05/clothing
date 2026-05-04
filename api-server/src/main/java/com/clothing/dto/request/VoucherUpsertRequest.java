package com.clothing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherUpsertRequest {

    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code max length is 50")
    private String code;

    @NotBlank(message = "discountType is required")
    @Size(max = 20, message = "discountType max length is 20")
    private String discountType;

    @NotNull(message = "discountValue is required")
    @Min(value = 1, message = "discountValue must be >= 1")
    private Long discountValue;

    @NotNull(message = "minOrderValue is required")
    @Min(value = 0, message = "minOrderValue must be >= 0")
    private Long minOrderValue;

    @NotNull(message = "maxUsage is required")
    @Min(value = 1, message = "maxUsage must be >= 1")
    private Integer maxUsage;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @NotBlank(message = "status is required")
    @Size(max = 20, message = "status max length is 20")
    private String status;
}
