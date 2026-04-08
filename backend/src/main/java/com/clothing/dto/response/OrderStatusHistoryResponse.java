package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderStatusHistoryResponse {

    private String status;
    private LocalDateTime changedAt;
}
