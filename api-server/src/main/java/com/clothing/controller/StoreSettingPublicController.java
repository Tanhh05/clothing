package com.clothing.controller;

import com.clothing.dto.response.StoreSettingsResponse;
import com.clothing.service.StoreSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store-settings")
public class StoreSettingPublicController {

    private final StoreSettingService storeSettingService;

    public StoreSettingPublicController(StoreSettingService storeSettingService) {
        this.storeSettingService = storeSettingService;
    }

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> getSettings() {
        return ResponseEntity.ok(storeSettingService.getSettings());
    }
}
