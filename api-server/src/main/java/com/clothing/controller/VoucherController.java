package com.clothing.controller;

import com.clothing.dto.response.VoucherBestResponse;
import com.clothing.dto.response.VoucherResponse;
import com.clothing.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getPublicActive() {
        return ResponseEntity.ok(voucherService.getPublicActive());
    }

    @GetMapping("/best")
    public ResponseEntity<VoucherBestResponse> suggestBest(@RequestParam Long subTotal) {
        return ResponseEntity.ok(voucherService.suggestBest(subTotal));
    }
}
