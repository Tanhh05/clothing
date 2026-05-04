package com.clothing.controller;

import com.clothing.dto.request.StoreSettingsUpsertRequest;
import com.clothing.dto.response.StoreSettingsResponse;
import com.clothing.service.StoreSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/store-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreSettingController {

    private final StoreSettingService storeSettingService;

    public AdminStoreSettingController(StoreSettingService storeSettingService) {
        this.storeSettingService = storeSettingService;
    }

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> getSettings() {
        return ResponseEntity.ok(storeSettingService.getSettings());
    }

    @PutMapping
    public ResponseEntity<StoreSettingsResponse> update(@Valid @RequestBody StoreSettingsUpsertRequest request) {
        return ResponseEntity.ok(storeSettingService.updateSettings(request));
    }
}
