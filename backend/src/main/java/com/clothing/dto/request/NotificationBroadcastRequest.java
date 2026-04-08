package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationBroadcastRequest {

    @NotBlank(message = "audience is required")
    @Size(max = 30, message = "audience max length is 30")
    private String audience;

    @NotBlank(message = "channel is required")
    @Size(max = 30, message = "channel max length is 30")
    private String channel;

    @NotBlank(message = "sendMode is required")
    @Size(max = 20, message = "sendMode max length is 20")
    private String sendMode;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title max length is 255")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    private LocalDateTime scheduledAt;
}
