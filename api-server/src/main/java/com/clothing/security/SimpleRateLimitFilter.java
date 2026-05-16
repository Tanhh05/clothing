package com.clothing.security;

import com.clothing.config.RequestMetaResolver;
import com.clothing.dto.response.ApiEnvelopeResponse;
import com.clothing.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String ORDER_CREATE_PATH = "/api/orders";

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public SimpleRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${app.security.rate-limit.login.max-requests:20}")
    private int loginMaxRequests;

    @Value("${app.security.rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    @Value("${app.security.rate-limit.register.max-requests:10}")
    private int registerMaxRequests;

    @Value("${app.security.rate-limit.register.window-seconds:60}")
    private int registerWindowSeconds;

    @Value("${app.security.rate-limit.order-create.max-requests:30}")
    private int orderCreateMaxRequests;

    @Value("${app.security.rate-limit.order-create.window-seconds:60}")
    private int orderCreateWindowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveClientKey(request, rule.keyPrefix());
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(System.currentTimeMillis()));
        long now = System.currentTimeMillis();
        if (now - counter.windowStartMs > (long) rule.windowSeconds() * 1000L) {
            counter.windowStartMs = now;
            counter.count.set(0);
        }
        int current = counter.count.incrementAndGet();
        if (current > rule.maxRequests()) {
            writeRateLimitedResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path)) {
            return new RateLimitRule("login", loginMaxRequests, loginWindowSeconds);
        }
        if ("POST".equalsIgnoreCase(method) && REGISTER_PATH.equals(path)) {
            return new RateLimitRule("register", registerMaxRequests, registerWindowSeconds);
        }
        if ("POST".equalsIgnoreCase(method) && ORDER_CREATE_PATH.equals(path)) {
            return new RateLimitRule("order-create", orderCreateMaxRequests, orderCreateWindowSeconds);
        }
        return null;
    }

    private String resolveClientKey(HttpServletRequest request, String prefix) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String ip;
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            ip = xForwardedFor.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return prefix + ":" + ip;
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse data = ApiErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .code("RATE_LIMITED")
                .error("Too Many Requests")
                .message("Too many requests. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(Map.of())
                .build();

        ApiEnvelopeResponse<ApiErrorResponse> body = ApiEnvelopeResponse.<ApiErrorResponse>builder()
                .meta(RequestMetaResolver.resolve(request))
                .data(data)
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class WindowCounter {
        private volatile long windowStartMs;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }

    private record RateLimitRule(String keyPrefix, int maxRequests, int windowSeconds) {
    }
}
