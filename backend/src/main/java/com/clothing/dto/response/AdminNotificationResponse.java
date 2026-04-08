package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminNotificationResponse {

    private Long id;
    private LocalDateTime createdAt;
    private String title;
    private String audience;
    private String channel;
    private String status;
}
