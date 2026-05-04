package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserNotificationResponse {

    private Long id;
    private String title;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
