package com.clothing.controller;

import com.clothing.dto.request.ReturnStatusUpdateRequest;
import com.clothing.dto.response.ReturnRequestResponse;
import com.clothing.service.ReturnRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnController {

    private final ReturnRequestService returnRequestService;

    public AdminReturnController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping
    public ResponseEntity<List<ReturnRequestResponse>> getAll(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(returnRequestService.getAll(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReturnRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReturnStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(returnRequestService.updateStatus(id, request.getStatus(), request.getNote()));
    }
}
