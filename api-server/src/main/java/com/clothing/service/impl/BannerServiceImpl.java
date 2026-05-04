package com.clothing.service.impl;

import com.clothing.dto.request.BannerUpsertRequest;
import com.clothing.dto.response.BannerResponse;
import com.clothing.entity.BannerEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.BannerRepository;
import com.clothing.service.BannerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BannerServiceImpl implements BannerService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final BannerRepository bannerRepository;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public List<BannerResponse> getPublicBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findAllVisibleByStatusOrderByIdDesc(STATUS_ACTIVE)
                .stream()
                .filter(entity -> isWithinSchedule(entity, now))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllVisibleOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse create(BannerUpsertRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BusinessException("imageUrl is required", HttpStatus.BAD_REQUEST);
        }
        BannerEntity entity = new BannerEntity();
        applyRequest(entity, request);
        return toResponse(bannerRepository.save(entity));
    }

    @Override
    @Transactional
    public BannerResponse update(Long id, BannerUpsertRequest request) {
        BannerEntity entity = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Banner not found", HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException("Banner not found", HttpStatus.NOT_FOUND);
        }
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            request.setImageUrl(entity.getImageUrl());
        }
        applyRequest(entity, request);
        return toResponse(bannerRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BannerEntity entity = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Banner not found", HttpStatus.NOT_FOUND));
        entity.setDeleted(true);
        bannerRepository.save(entity);
    }

    private void applyRequest(BannerEntity entity, BannerUpsertRequest request) {
        validateSchedule(request.getStartAt(), request.getEndAt());
        entity.setTitle(trimToNull(request.getTitle()));
        entity.setImageUrl(request.getImageUrl().trim());
        entity.setLinkUrl(trimToNull(request.getLinkUrl()));
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setDeleted(false);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_DRAFT;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("INACTIVE".equals(normalized)) {
            return STATUS_DRAFT;
        }
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DRAFT.equals(normalized)) {
            throw new BusinessException("status must be DRAFT or ACTIVE", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new BusinessException("endAt must be greater than or equal to startAt", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isWithinSchedule(BannerEntity entity, LocalDateTime now) {
        LocalDateTime startAt = entity.getStartAt();
        LocalDateTime endAt = entity.getEndAt();
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }
        return true;
    }

    private String resolveDisplayStatus(BannerEntity entity) {
        String baseStatus = normalizeStatus(entity.getStatus());
        if (STATUS_DRAFT.equals(baseStatus)) {
            return STATUS_DRAFT;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = entity.getStartAt();
        LocalDateTime endAt = entity.getEndAt();
        if (startAt != null && now.isBefore(startAt)) {
            return STATUS_SCHEDULED;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return STATUS_EXPIRED;
        }
        return STATUS_ACTIVE;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BannerResponse toResponse(BannerEntity entity) {
        return BannerResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .imageUrl(entity.getImageUrl())
                .linkUrl(entity.getLinkUrl())
                .status(resolveDisplayStatus(entity))
                .baseStatus(normalizeStatus(entity.getStatus()))
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
