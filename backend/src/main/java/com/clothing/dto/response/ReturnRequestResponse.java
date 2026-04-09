package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReturnRequestResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private String customer;
    private String returnType;
    private String reasonCode;
    private String reasonDetail;
    private String evidenceUrls;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private List<ReturnRequestItemResponse> items;
}
