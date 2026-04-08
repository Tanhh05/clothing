package com.clothing.service.impl;

import com.clothing.dto.response.AuditLogResponse;
import com.clothing.entity.AuditLogEntity;
import com.clothing.repository.AuditLogRepository;
import com.clothing.service.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String detail) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActor(resolveActor());
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDetail(detail);
        entity.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(entity);
    }

    @Override
    public List<AuditLogResponse> getRecentLogs() {
        return auditLogRepository.findTop200ByOrderByIdDesc()
                .stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .actor(log.getActor())
                        .action(log.getAction())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .detail(log.getDetail())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}

