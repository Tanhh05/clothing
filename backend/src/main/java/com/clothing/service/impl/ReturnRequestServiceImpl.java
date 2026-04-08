package com.clothing.service.impl;

import com.clothing.dto.response.ReturnRequestResponse;
import com.clothing.entity.ReturnRequestEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.ReturnRequestRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.ReturnRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final AuditLogService auditLogService;

    public ReturnRequestServiceImpl(ReturnRequestRepository returnRequestRepository, AuditLogService auditLogService) {
        this.returnRequestRepository = returnRequestRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ReturnRequestResponse> getAll(String status) {
        if (status == null || status.isBlank()) {
            return returnRequestRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return returnRequestRepository.findByStatusOrderByIdDesc(normalized).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateStatus(Long id, String status) {
        ReturnRequestEntity entity = returnRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Return request not found", HttpStatus.NOT_FOUND));
        String next = status.trim().toUpperCase(Locale.ROOT);
        validateTransition(entity.getStatus(), next);

        entity.setStatus(next);
        if ("REJECTED".equals(next) || "REFUNDED".equals(next)) {
            entity.setResolvedAt(LocalDateTime.now());
        }
        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        auditLogService.log("RETURN_STATUS_UPDATED", "RETURN_REQUEST", saved.getId(), "Updated status to " + next);
        return toResponse(saved);
    }

    private void validateTransition(String currentStatus, String nextStatus) {
        String current = currentStatus == null ? "REQUESTED" : currentStatus.trim().toUpperCase(Locale.ROOT);
        if ("REQUESTED".equals(current)) {
            if ("APPROVED".equals(nextStatus) || "REJECTED".equals(nextStatus)) {
                return;
            }
            throw new BusinessException("Invalid transition from REQUESTED to " + nextStatus, HttpStatus.BAD_REQUEST);
        }
        if ("APPROVED".equals(current)) {
            if ("REFUNDED".equals(nextStatus)) {
                return;
            }
            throw new BusinessException("Invalid transition from APPROVED to " + nextStatus, HttpStatus.BAD_REQUEST);
        }
        throw new BusinessException("Cannot transition from " + current, HttpStatus.BAD_REQUEST);
    }

    private ReturnRequestResponse toResponse(ReturnRequestEntity entity) {
        return ReturnRequestResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .customer(entity.getCustomerName())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .build();
    }
}
