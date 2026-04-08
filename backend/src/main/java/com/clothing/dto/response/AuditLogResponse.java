package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuditLogResponse {

    private Long id;
    private String actor;
    private String action;
    private String entityType;
    private Long entityId;
    private String detail;
    private LocalDateTime createdAt;
}

