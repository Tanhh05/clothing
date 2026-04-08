package com.clothing.service.impl;

import com.clothing.dto.request.VoucherUpsertRequest;
import com.clothing.dto.response.VoucherResponse;
import com.clothing.entity.CouponEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.CouponRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.VoucherService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final CouponRepository couponRepository;
    private final AuditLogService auditLogService;

    public VoucherServiceImpl(CouponRepository couponRepository, AuditLogService auditLogService) {
        this.couponRepository = couponRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<VoucherResponse> getAll() {
        return couponRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherUpsertRequest request) {
        validateRequest(request, null);
        CouponEntity entity = new CouponEntity();
        apply(entity, request);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUsedCount(0);
        CouponEntity saved = couponRepository.save(entity);
        auditLogService.log("VOUCHER_CREATED", "COUPON", saved.getId(), "Created voucher " + saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponse update(Long id, VoucherUpsertRequest request) {
        CouponEntity entity = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher not found", HttpStatus.NOT_FOUND));
        validateRequest(request, id);
        apply(entity, request);
        CouponEntity saved = couponRepository.save(entity);
        auditLogService.log("VOUCHER_UPDATED", "COUPON", id, "Updated voucher " + saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CouponEntity entity = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher not found", HttpStatus.NOT_FOUND));
        couponRepository.delete(entity);
        auditLogService.log("VOUCHER_DELETED", "COUPON", id, "Deleted voucher " + entity.getCode());
    }

    private void apply(CouponEntity entity, VoucherUpsertRequest request) {
        entity.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setDiscountType(request.getDiscountType().trim().toUpperCase(Locale.ROOT));
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMinOrderValue(request.getMinOrderValue());
        entity.setQuantity(request.getMaxUsage());
        entity.setStartDate(request.getStartAt());
        entity.setEndDate(request.getEndAt());
        entity.setStatus(request.getStatus().trim().toUpperCase(Locale.ROOT));
    }

    private void validateRequest(VoucherUpsertRequest request, Long id) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (request.getStartAt() != null && request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException("endAt must be after startAt", HttpStatus.BAD_REQUEST);
        }
        CouponEntity byCode = couponRepository.findByCodeIgnoreCase(code).orElse(null);
        if (byCode != null && (id == null || !byCode.getId().equals(id))) {
            throw new BusinessException("Voucher code already exists", HttpStatus.BAD_REQUEST);
        }
        String discountType = request.getDiscountType().trim().toUpperCase(Locale.ROOT);
        if (!"PERCENT".equals(discountType) && !"AMOUNT".equals(discountType)) {
            throw new BusinessException("discountType must be PERCENT or AMOUNT", HttpStatus.BAD_REQUEST);
        }
    }

    private VoucherResponse toResponse(CouponEntity entity) {
        return VoucherResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .minOrderValue(entity.getMinOrderValue())
                .maxUsage(entity.getQuantity())
                .startAt(entity.getStartDate())
                .endAt(entity.getEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
