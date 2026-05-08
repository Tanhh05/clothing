package com.clothing.config;

import com.clothing.dto.response.ResponseMeta;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class RequestMetaResolver {

    private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    private static final String HEADER_CURRENCY = "X-Currency";

    private RequestMetaResolver() {
    }

    public static ResponseMeta resolve(HttpServletRequest request) {
        return ResponseMeta.builder()
                .language(resolveLanguage(request))
                .currency(resolveCurrency(request))
                .build();
    }

    public static String resolveLanguage(HttpServletRequest request) {
        String header = request == null ? null : request.getHeader(HEADER_ACCEPT_LANGUAGE);
        if (!StringUtils.hasText(header)) {
            return "vi";
        }
        String normalized = header.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("en")) {
            return "en";
        }
        if (normalized.startsWith("vi")) {
            return "vi";
        }
        if (normalized.startsWith("my")) {
            return "my";
        }
        return "vi";
    }

    public static String resolveCurrency(HttpServletRequest request) {
        String header = request == null ? null : request.getHeader(HEADER_CURRENCY);
        if (!StringUtils.hasText(header)) {
            return "VND";
        }
        String normalized = header.trim().toUpperCase(Locale.ROOT);
        if ("MMK".equals(normalized)) {
            return "MYN";
        }
        if ("USD".equals(normalized) || "VND".equals(normalized) || "MYN".equals(normalized)) {
            return normalized;
        }
        return "VND";
    }
}
