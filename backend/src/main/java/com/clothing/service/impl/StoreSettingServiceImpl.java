package com.clothing.service.impl;

import com.clothing.dto.request.StoreSettingsUpsertRequest;
import com.clothing.dto.response.StoreSettingsResponse;
import com.clothing.entity.StoreSettingEntity;
import com.clothing.repository.StoreSettingRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.StoreSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreSettingServiceImpl implements StoreSettingService {

    private static final String STORE_NAME = "storeName";
    private static final String HOTLINE = "hotline";
    private static final String SUPPORT_EMAIL = "supportEmail";
    private static final String ADDRESS = "address";
    private static final String DEFAULT_SHIPPING_FEE = "defaultShippingFee";
    private static final String FREE_SHIPPING_THRESHOLD = "freeShippingThreshold";
    private static final String ENABLE_COD = "enableCOD";
    private static final String ENABLE_MOMO = "enableMomo";
    private static final String SHIPPING_POLICY = "shippingPolicy";
    private static final String RETURN_POLICY = "returnPolicy";

    private final StoreSettingRepository storeSettingRepository;
    private final AuditLogService auditLogService;

    public StoreSettingServiceImpl(StoreSettingRepository storeSettingRepository, AuditLogService auditLogService) {
        this.storeSettingRepository = storeSettingRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public StoreSettingsResponse getSettings() {
        Map<String, String> settings = loadSettingsMap();
        return toResponse(settings);
    }

    @Override
    @Transactional
    public StoreSettingsResponse updateSettings(StoreSettingsUpsertRequest request) {
        upsert(STORE_NAME, request.getStoreName().trim());
        upsert(HOTLINE, request.getHotline().trim());
        upsert(SUPPORT_EMAIL, request.getSupportEmail().trim());
        upsert(ADDRESS, request.getAddress().trim());
        upsert(DEFAULT_SHIPPING_FEE, String.valueOf(request.getDefaultShippingFee()));
        upsert(FREE_SHIPPING_THRESHOLD, String.valueOf(request.getFreeShippingThreshold()));
        upsert(ENABLE_COD, String.valueOf(request.getEnableCOD()));
        upsert(ENABLE_MOMO, String.valueOf(request.getEnableMomo()));
        upsert(SHIPPING_POLICY, request.getShippingPolicy().trim());
        upsert(RETURN_POLICY, request.getReturnPolicy().trim());
        auditLogService.log("STORE_SETTINGS_UPDATED", "STORE_SETTING", null, "Updated store settings");
        return getSettings();
    }

    private void upsert(String key, String value) {
        StoreSettingEntity entity = storeSettingRepository.findBySettingKey(key).orElseGet(StoreSettingEntity::new);
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        storeSettingRepository.save(entity);
    }

    private Map<String, String> loadSettingsMap() {
        List<StoreSettingEntity> entities = storeSettingRepository.findAllByOrderBySettingKeyAsc();
        Map<String, String> settings = new HashMap<>();
        settings.put(STORE_NAME, "Clothing Store");
        settings.put(HOTLINE, "0900 000 000");
        settings.put(SUPPORT_EMAIL, "support@clothing.local");
        settings.put(ADDRESS, "TP.HCM, Việt Nam");
        settings.put(DEFAULT_SHIPPING_FEE, "30000");
        settings.put(FREE_SHIPPING_THRESHOLD, "500000");
        settings.put(ENABLE_COD, "true");
        settings.put(ENABLE_MOMO, "true");
        settings.put(SHIPPING_POLICY, "Giao hàng toàn quốc trong 2-5 ngày làm việc.");
        settings.put(RETURN_POLICY, "Đổi trả trong 7 ngày với sản phẩm còn nguyên tem.");
        for (StoreSettingEntity entity : entities) {
            settings.put(entity.getSettingKey(), entity.getSettingValue());
        }
        return settings;
    }

    private StoreSettingsResponse toResponse(Map<String, String> settings) {
        return StoreSettingsResponse.builder()
                .storeName(settings.get(STORE_NAME))
                .hotline(settings.get(HOTLINE))
                .supportEmail(settings.get(SUPPORT_EMAIL))
                .address(settings.get(ADDRESS))
                .defaultShippingFee(parseLong(settings.get(DEFAULT_SHIPPING_FEE), 30000L))
                .freeShippingThreshold(parseLong(settings.get(FREE_SHIPPING_THRESHOLD), 500000L))
                .enableCOD(parseBoolean(settings.get(ENABLE_COD), true))
                .enableMomo(parseBoolean(settings.get(ENABLE_MOMO), true))
                .shippingPolicy(settings.get(SHIPPING_POLICY))
                .returnPolicy(settings.get(RETURN_POLICY))
                .build();
    }

    private Long parseLong(String value, Long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Boolean parseBoolean(String value, Boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
