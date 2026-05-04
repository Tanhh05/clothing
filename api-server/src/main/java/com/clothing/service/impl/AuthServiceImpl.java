package com.clothing.service.impl;

import com.clothing.dto.request.LoginRequest;
import com.clothing.dto.request.LogoutRequest;
import com.clothing.dto.request.RefreshTokenRequest;
import com.clothing.dto.request.RegisterRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.request.ChangePasswordRequest;
import com.clothing.dto.response.AuthResponse;
import com.clothing.dto.response.VerifyResetOtpResponse;
import com.clothing.entity.PasswordResetOtpEntity;
import com.clothing.entity.RefreshTokenEntity;
import com.clothing.dto.response.UserResponse;
import com.clothing.entity.RoleEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.mapper.UserMapper;
import com.clothing.repository.PasswordResetOtpRepository;
import com.clothing.repository.RefreshTokenRepository;
import com.clothing.repository.RoleRepository;
import com.clothing.repository.UserRepository;
import com.clothing.security.JwtProperties;
import com.clothing.security.JwtService;
import com.clothing.security.GoogleIdTokenVerifier;
import com.clothing.service.AuthService;
import com.clothing.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String USER_ROLE = "USER";
    private static final String RESET_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String OTP_DIGITS = "0123456789";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRE_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserMapper userMapper,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }

        RoleEntity userRole = roleRepository.findByName(USER_ROLE)
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName(USER_ROLE);
                    return roleRepository.save(role);
                });

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus("ACTIVE");
        user.getRoles().add(userRole);

        UserEntity savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        UserEntity user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        ensureUserActive(user);

        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo = googleIdTokenVerifier.verify(idToken);
        UserEntity user = userRepository.findByEmail(googleUserInfo.email())
                .orElseGet(() -> createGoogleUser(googleUserInfo));
        user = syncGoogleProfile(user, googleUserInfo);
        ensureUserActive(user);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = normalizeToken(request.getRefreshToken());
        String tokenHash = hashRefreshToken(refreshToken);

        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
        if (Boolean.TRUE.equals(storedToken.getRevoked())) {
            throw new BusinessException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));
        ensureUserActive(user);

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = normalizeToken(request.getRefreshToken());
        String tokenHash = hashRefreshToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (storedToken == null || Boolean.TRUE.equals(storedToken.getRevoked())) {
            return;
        }

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new BusinessException("email is required", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Email does not exist", HttpStatus.NOT_FOUND));

        PasswordResetOtpEntity latest = passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElse(null);
        if (latest != null && !Boolean.TRUE.equals(latest.getUsed()) && latest.getExpiresAt().isAfter(LocalDateTime.now())) {
            long secondsLeft = java.time.Duration.between(LocalDateTime.now(), latest.getExpiresAt()).getSeconds();
            if (secondsLeft > 120) {
                throw new BusinessException("Please wait before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        String otp = generateDigits(OTP_LENGTH);
        PasswordResetOtpEntity entity = new PasswordResetOtpEntity();
        entity.setUserId(user.getId());
        entity.setEmail(normalizedEmail);
        entity.setOtpHash(sha256(otp));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES));
        entity.setAttemptCount(0);
        entity.setMaxAttempts(OTP_MAX_ATTEMPTS);
        entity.setUsed(false);
        passwordResetOtpRepository.save(entity);

        emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
        log.info("Password reset OTP sent for userId={} email={}", user.getId(), normalizedEmail);
    }

    @Override
    @Transactional
    public VerifyResetOtpResponse verifyForgotPasswordOtp(String email, String otp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedOtp = otp == null ? "" : otp.trim();
        if (normalizedEmail.isBlank() || normalizedOtp.isBlank()) {
            throw new BusinessException("email and otp are required", HttpStatus.BAD_REQUEST);
        }

        PasswordResetOtpEntity entity = passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BusinessException("OTP not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(entity.getUsed())) {
            throw new BusinessException("OTP has already been used", HttpStatus.BAD_REQUEST);
        }
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired", HttpStatus.BAD_REQUEST);
        }
        if (entity.getAttemptCount() >= entity.getMaxAttempts()) {
            throw new BusinessException("OTP attempts exceeded", HttpStatus.TOO_MANY_REQUESTS);
        }

        String otpHash = sha256(normalizedOtp);
        if (!otpHash.equals(entity.getOtpHash())) {
            entity.setAttemptCount(entity.getAttemptCount() + 1);
            passwordResetOtpRepository.save(entity);
            throw new BusinessException("OTP is incorrect", HttpStatus.BAD_REQUEST);
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setResetTokenHash(sha256(resetToken));
        entity.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRE_MINUTES));
        passwordResetOtpRepository.save(entity);

        return VerifyResetOtpResponse.builder()
                .resetToken(resetToken)
                .expiresInSeconds(RESET_TOKEN_EXPIRE_MINUTES * 60L)
                .build();
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(String email, String resetToken, String newPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedToken = resetToken == null ? "" : resetToken.trim();
        String normalizedPassword = newPassword == null ? "" : newPassword.trim();
        if (normalizedEmail.isBlank() || normalizedToken.isBlank() || normalizedPassword.isBlank()) {
            throw new BusinessException("email, resetToken and newPassword are required", HttpStatus.BAD_REQUEST);
        }
        if (normalizedPassword.length() < 6) {
            throw new BusinessException("newPassword must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }

        PasswordResetOtpEntity entity = passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Reset request not found", HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(entity.getUsed())) {
            throw new BusinessException("Reset request has already been used", HttpStatus.BAD_REQUEST);
        }
        if (entity.getResetTokenHash() == null || entity.getResetTokenExpiresAt() == null) {
            throw new BusinessException("OTP is not verified", HttpStatus.BAD_REQUEST);
        }
        if (entity.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Reset token has expired", HttpStatus.BAD_REQUEST);
        }
        if (!sha256(normalizedToken).equals(entity.getResetTokenHash())) {
            throw new BusinessException("Reset token is invalid", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Email does not exist", HttpStatus.NOT_FOUND));
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        userRepository.save(user);

        entity.setUsed(true);
        passwordResetOtpRepository.save(entity);
        log.info("Password has been reset with OTP for userId={} email={}", user.getId(), normalizedEmail);
    }

    @Override
    public UserResponse getCurrentUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(String username, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (request.getUsername() != null) {
            String nextUsername = request.getUsername().trim();
            if (!nextUsername.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(nextUsername)) {
                throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
            }
            user.setUsername(nextUsername);
        }
        if (request.getEmail() != null) {
            String nextEmail = request.getEmail().trim();
            if (!nextEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(nextEmail)) {
                throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(nextEmail);
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        String currentPassword = request.getCurrentPassword() == null ? "" : request.getCurrentPassword().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword().trim();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("New password must be different from current password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role == null ? null : role.getName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
        if (roles.isEmpty()) {
            roles = Set.of(USER_ROLE);
        }

        String accessToken = jwtService.generateToken(
                user.getUsername(),
                Map.of("roles", roles, "userId", user.getId())
        );
        String refreshToken = generateAndPersistRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationSeconds())
                .refreshExpiresIn(jwtProperties.getRefreshExpirationSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(roles)
                .build();
    }

    private UserEntity createGoogleUser(GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo) {
        RoleEntity userRole = roleRepository.findByName(USER_ROLE)
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName(USER_ROLE);
                    return roleRepository.save(role);
                });

        UserEntity user = new UserEntity();
        user.setEmail(googleUserInfo.email());
        user.setUsername(resolveGoogleUsername(googleUserInfo.email()));
        user.setFullName(googleUserInfo.name());
        user.setPhone(googleUserInfo.phone());
        user.setStatus("ACTIVE");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    private UserEntity syncGoogleProfile(UserEntity user, GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo) {
        boolean dirty = false;

        String nextEmail = googleUserInfo.email();
        if (nextEmail != null && !nextEmail.isBlank() && !nextEmail.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(nextEmail.trim());
            dirty = true;
        }

        String nextFullName = googleUserInfo.name();
        if (nextFullName != null) {
            nextFullName = nextFullName.trim();
            String currentFullName = user.getFullName() == null ? "" : user.getFullName().trim();
            if (!nextFullName.isBlank() && !nextFullName.equals(currentFullName)) {
                user.setFullName(nextFullName);
                dirty = true;
            }
        }

        String nextPhone = googleUserInfo.phone();
        if (nextPhone != null) {
            nextPhone = nextPhone.trim();
            String currentPhone = user.getPhone() == null ? "" : user.getPhone().trim();
            if (!nextPhone.isBlank() && !nextPhone.equals(currentPhone)) {
                user.setPhone(nextPhone);
                dirty = true;
            }
        }

        return dirty ? userRepository.save(user) : user;
    }

    private String resolveGoogleUsername(String email) {
        String base = email.split("@")[0]
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "google_user";
        }

        String candidate = base;
        int index = 0;
        while (userRepository.existsByUsername(candidate)) {
            index++;
            candidate = base + "_" + index;
        }
        return candidate;
    }

    private String generateAndPersistRefreshToken(Long userId) {
        String rawToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setUserId(userId);
        tokenEntity.setTokenHash(hashRefreshToken(rawToken));
        tokenEntity.setRevoked(false);
        tokenEntity.setCreatedAt(LocalDateTime.now());
        tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationSeconds()));
        refreshTokenRepository.save(tokenEntity);
        return rawToken;
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException("Cannot process refresh token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String normalizeToken(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isBlank()) {
            throw new BusinessException("refreshToken is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void ensureUserActive(UserEntity user) {
        String status = user.getStatus() == null ? "ACTIVE" : user.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status)) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }
    }

    private String generateTemporaryPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(RESET_PASSWORD_CHARS.length());
            sb.append(RESET_PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private String generateDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(OTP_DIGITS.length());
            sb.append(OTP_DIGITS.charAt(idx));
        }
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException("Cannot process secure token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
