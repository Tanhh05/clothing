package com.clothing.controller;

import com.clothing.dto.request.GoogleLoginRequest;
import com.clothing.dto.request.ForgotPasswordRequest;
import com.clothing.dto.request.LoginRequest;
import com.clothing.dto.request.LogoutRequest;
import com.clothing.dto.request.ResetPasswordWithOtpRequest;
import com.clothing.dto.request.RefreshTokenRequest;
import com.clothing.dto.request.RegisterRequest;
import com.clothing.dto.request.TestEmailRequest;
import com.clothing.dto.request.VerifyResetOtpRequest;
import com.clothing.dto.response.AuthResponse;
import com.clothing.dto.response.TestEmailResponse;
import com.clothing.dto.response.VerifyResetOtpResponse;
import com.clothing.service.AuthService;
import com.clothing.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.getIdToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<VerifyResetOtpResponse> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyResetOtpRequest request
    ) {
        return ResponseEntity.ok(authService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp()));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Void> resetPasswordWithOtp(
            @Valid @RequestBody ResetPasswordWithOtpRequest request
    ) {
        authService.resetPasswordWithOtp(request.getEmail(), request.getResetToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-email")
    public ResponseEntity<TestEmailResponse> testEmail(@Valid @RequestBody TestEmailRequest request) {
        emailService.sendTestEmail(request.getEmail(), request.getSubject(), request.getContent());
        return ResponseEntity.ok(TestEmailResponse.builder()
                .success(true)
                .message("Email sent")
                .to(request.getEmail())
                .build());
    }
}
