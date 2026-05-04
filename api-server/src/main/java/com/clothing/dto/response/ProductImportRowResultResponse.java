package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductImportRowResultResponse {
    private Integer rowNumber;
    private String name;
    private Long productId;
    private String status;
    private String message;
}

