package com.clothing.service;

import com.clothing.dto.request.WarehouseInboundCreateRequest;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.WarehouseInboundDetailResponse;
import com.clothing.dto.response.WarehouseInboundResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface WarehouseInboundService {

    List<WarehouseInboundResponse> getAll();

    PageResponse<WarehouseInboundResponse> getPage(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            LocalDateTime from,
            LocalDateTime to
    );

    WarehouseInboundDetailResponse getById(Long id);

    List<String> getSkuSuggestions(String q);

    WarehouseInboundResponse create(WarehouseInboundCreateRequest request);
}
