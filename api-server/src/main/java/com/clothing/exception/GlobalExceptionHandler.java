package com.clothing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        return buildError(ex.getStatus(), statusCodeKey(ex.getStatus()), ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid username/email or password", request.getRequestURI(), null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabledUser(DisabledException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", "User account is inactive", request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthRequired(AuthenticationCredentialsNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required", request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed", request.getRequestURI(), null);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource", request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage(),
                        (v1, v2) -> v1
                ));
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request body", request.getRequestURI(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "PAYLOAD_TOO_LARGE",
                "Kích thước file vượt quá giới hạn cho phép",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "MULTIPART_ERROR",
                "Yêu cầu upload không hợp lệ hoặc file quá lớn",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({NoResourceFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy dữ liệu", request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request.getRequestURI(), null);
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .code(code)
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String statusCodeKey(HttpStatus status) {
        if (status == null) {
            return "BUSINESS_ERROR";
        }
        if (status.is4xxClientError()) {
            return status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "BUSINESS_BAD_REQUEST";
        }
        if (status.is5xxServerError()) {
            return "BUSINESS_SERVER_ERROR";
        }
        return "BUSINESS_ERROR";
    }
}
