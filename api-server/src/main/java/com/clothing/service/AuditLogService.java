package com.clothing.service;

import com.clothing.dto.response.AuditLogResponse;

import java.util.List;

public interface AuditLogService {

    void log(String action, String entityType, Long entityId, String detail);

    List<AuditLogResponse> getRecentLogs();
}

