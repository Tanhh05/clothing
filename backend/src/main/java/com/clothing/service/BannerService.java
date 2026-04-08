package com.clothing.service;

import com.clothing.dto.request.BannerUpsertRequest;
import com.clothing.dto.response.BannerResponse;

import java.util.List;

public interface BannerService {

    List<BannerResponse> getPublicBanners();

    List<BannerResponse> getAllBanners();

    BannerResponse create(BannerUpsertRequest request);

    BannerResponse update(Long id, BannerUpsertRequest request);

    void delete(Long id);
}
