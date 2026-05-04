package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestEmailResponse {
    private boolean success;
    private String message;
    private String to;
}
