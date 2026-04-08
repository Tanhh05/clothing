package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReturnRequestResponse {

    private Long id;
    private Long orderId;
    private String customer;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
}
