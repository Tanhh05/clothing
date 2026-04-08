package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class UploadPresignResponse {

    private String objectKey;
    private String uploadUrl;
    private String fileUrl;
    private String method;
    private long expiresInSeconds;
    private Map<String, String> requiredHeaders;
}
