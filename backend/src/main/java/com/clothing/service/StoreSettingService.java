package com.clothing.service;

import com.clothing.dto.request.StoreSettingsUpsertRequest;
import com.clothing.dto.response.StoreSettingsResponse;

public interface StoreSettingService {

    StoreSettingsResponse getSettings();

    StoreSettingsResponse updateSettings(StoreSettingsUpsertRequest request);
}
