package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyResetOtpResponse {

    private String resetToken;
    private long expiresInSeconds;
}
