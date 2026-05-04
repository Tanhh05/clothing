package com.clothing.controller;

import com.clothing.dto.request.VoucherUpsertRequest;
import com.clothing.dto.response.VoucherResponse;
import com.clothing.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getAll() {
        return ResponseEntity.ok(voucherService.getAll());
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> create(@Valid @RequestBody VoucherUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherResponse> update(@PathVariable Long id, @Valid @RequestBody VoucherUpsertRequest request) {
        return ResponseEntity.ok(voucherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
