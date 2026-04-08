package com.clothing.service.impl;

import com.clothing.dto.request.LoginRequest;
import com.clothing.dto.request.RegisterRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.response.AuthResponse;
import com.clothing.dto.response.UserResponse;
import com.clothing.entity.RoleEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.mapper.UserMapper;
import com.clothing.repository.RoleRepository;
import com.clothing.repository.UserRepository;
import com.clothing.security.JwtProperties;
import com.clothing.security.JwtService;
import com.clothing.security.GoogleIdTokenVerifier;
import com.clothing.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserMapper userMapper,
            GoogleIdTokenVerifier googleIdTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
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

        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo = googleIdTokenVerifier.verify(idToken);
        UserEntity user = userRepository.findByEmail(googleUserInfo.email())
                .orElseGet(() -> createGoogleUser(googleUserInfo));
        return toAuthResponse(user);
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

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        Set<String> roles = user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());
        String accessToken = jwtService.generateToken(
                user.getUsername(),
                Map.of("roles", roles, "userId", user.getId())
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationSeconds())
                .userId(user.getId())
                .username(user.getUsername())
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
        user.setStatus("ACTIVE");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.getRoles().add(userRole);
        return userRepository.save(user);
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
}
