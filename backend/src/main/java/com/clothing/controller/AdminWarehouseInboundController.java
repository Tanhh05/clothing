package com.clothing.controller;

import com.clothing.dto.request.WarehouseInboundCreateRequest;
import com.clothing.dto.response.WarehouseInboundResponse;
import com.clothing.service.WarehouseInboundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouse-inbounds")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWarehouseInboundController {

    private final WarehouseInboundService warehouseInboundService;

    public AdminWarehouseInboundController(WarehouseInboundService warehouseInboundService) {
        this.warehouseInboundService = warehouseInboundService;
    }

    @GetMapping
    public ResponseEntity<List<WarehouseInboundResponse>> getAll() {
        return ResponseEntity.ok(warehouseInboundService.getAll());
    }

    @PostMapping
    public ResponseEntity<WarehouseInboundResponse> create(@Valid @RequestBody WarehouseInboundCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseInboundService.create(request));
    }
}
