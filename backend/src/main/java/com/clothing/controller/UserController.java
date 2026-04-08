package com.clothing.controller;

import com.clothing.dto.request.UpdateUserStatusRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.UserResponse;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.mapper.UserMapper;
import com.clothing.service.AuthService;
import jakarta.validation.Valid;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clothing.repository.UserRepository;

import java.util.Locale;
import java.util.Map;

@RestController
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserController(AuthService authService, UserRepository userRepository, UserMapper userMapper) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @GetMapping("/api/user/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    @PutMapping("/api/user/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(authService.updateCurrentUser(authentication.getName(), request));
    }

    @GetMapping("/api/admin/ping")
    public ResponseEntity<Map<String, String>> adminPing() {
        return ResponseEntity.ok(Map.of("message", "Admin access granted"));
    }

    @GetMapping("/api/user/ping")
    public ResponseEntity<Map<String, String>> userPing() {
        return ResponseEntity.ok(Map.of("message", "User access granted"));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        if (page < 0) {
            throw new BusinessException("page must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException("size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSortBy));

        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim().toUpperCase(Locale.ROOT);

        Specification<UserEntity> specification = Specification.where(null);
        if (keyword != null) {
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> {
                Predicate byUsername = cb.like(cb.lower(root.get("username")), pattern);
                Predicate byEmail = cb.like(cb.lower(root.get("email")), pattern);
                Predicate byFullName = cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), pattern);
                Predicate byPhone = cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern);
                return cb.or(byUsername, byEmail, byFullName, byPhone);
            });
        }
        if (normalizedStatus != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(cb.coalesce(root.get("status"), "")), normalizedStatus));
        }

        Page<UserEntity> usersPage = userRepository.findAll(specification, pageable);

        return ResponseEntity.ok(PageResponse.<UserResponse>builder()
                .content(usersPage.getContent().stream().map(userMapper::toResponse).toList())
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .first(usersPage.isFirst())
                .last(usersPage.isLast())
                .build());
    }

    @PatchMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("status must be ACTIVE or INACTIVE", HttpStatus.BAD_REQUEST);
        }

        user.setStatus(status);
        UserEntity saved = userRepository.save(user);
        return ResponseEntity.ok(userMapper.toResponse(saved));
    }
}
