package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductImportResponse {
    private String batchId;
    private Integer totalRows;
    private Integer successCount;
    private Integer failedCount;
    private Boolean dryRun;
    private Boolean upsertBySku;
    private List<ProductImportRowResultResponse> results;
}
