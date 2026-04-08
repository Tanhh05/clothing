package com.clothing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.r2")
public class R2Properties {

    private String accountId;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String endpoint;
    private String region;
    private String publicBaseUrl;
    private long presignExpirationMinutes;
    private long maxFileSizeBytes;
    private String allowedContentTypes;
    private String imageCacheControl;

    public List<String> allowedContentTypeList() {
        String value = allowedContentTypes;
        if (value == null || value.isBlank()) {
            value = "image/jpeg,image/png,image/webp,image/avif";
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
