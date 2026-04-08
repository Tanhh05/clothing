package com.clothing.service;

import com.clothing.dto.request.WarehouseInboundCreateRequest;
import com.clothing.dto.response.WarehouseInboundResponse;

import java.util.List;

public interface WarehouseInboundService {

    List<WarehouseInboundResponse> getAll();

    WarehouseInboundResponse create(WarehouseInboundCreateRequest request);
}
