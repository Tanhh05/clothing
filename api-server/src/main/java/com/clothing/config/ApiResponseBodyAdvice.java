package com.clothing.config;

import com.clothing.dto.response.ApiEnvelopeResponse;
import com.clothing.dto.response.ResponseMeta;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Component
@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response
    ) {
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
            return body;
        }
        if (body == null || body instanceof ApiEnvelopeResponse<?>) {
            return body;
        }

        HttpServletRequest servletRequest = request instanceof ServletServerHttpRequest servletServerHttpRequest
                ? servletServerHttpRequest.getServletRequest()
                : null;
        ResponseMeta meta = RequestMetaResolver.resolve(servletRequest);

        return ApiEnvelopeResponse.builder()
                .meta(meta)
                .data(body)
                .build();
    }
}
