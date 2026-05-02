package com.clothing.controller;

import com.clothing.dto.request.UpdateUserStatusRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.request.ChangePasswordRequest;
import com.clothing.dto.request.OrderIdsRequest;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.UserResponse;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.mapper.UserMapper;
import com.clothing.service.AuthService;
import jakarta.validation.Valid;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.clothing.repository.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

    @PatchMapping("/api/user/me/password")
    public ResponseEntity<Void> changeMyPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changeCurrentUserPassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
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

        Specification<UserEntity> specification = buildUserSpecification(q, status);

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

    @org.springframework.web.bind.annotation.PostMapping("/api/admin/users/export-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersExcel(
            @Valid @RequestBody OrderIdsRequest request
    ) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        List<Long> ids = request.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }

        List<UserEntity> found = userRepository.findAllById(ids);
        Map<Long, UserEntity> byId = new LinkedHashMap<>();
        found.forEach(user -> byId.put(user.getId(), user));
        List<Long> missingIds = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException("Users not found: " + missingIds, HttpStatus.NOT_FOUND);
        }
        List<UserEntity> users = ids.stream().map(byId::get).toList();

        byte[] content = buildUsersExcel(users);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=khach-hang-" + ts + ".xlsx")
                .body(content);
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

    private Specification<UserEntity> buildUserSpecification(String q, String status) {
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
        return specification;
    }

    private byte[] buildUsersExcel(List<UserEntity> users) {
        List<UserEntity> safeUsers = users == null ? List.of() : users;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Customers");
            String[] headers = {"STT", "Ho ten", "Tai khoan", "Email", "So dien thoai", "Trang thai", "Vai tro"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (int i = 0; i < safeUsers.size(); i++) {
                UserEntity user = safeUsers.get(i);
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(safeText(user.getFullName()));
                row.createCell(2).setCellValue(safeText(user.getUsername()));
                row.createCell(3).setCellValue(safeText(user.getEmail()));
                row.createCell(4).setCellValue(safeText(user.getPhone()));
                row.createCell(5).setCellValue(safeText(user.getStatus()));
                row.createCell(6).setCellValue(String.join(", ", extractRoles(user)));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Cannot export users excel", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> extractRoles(UserEntity user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        user.getRoles().forEach(role -> {
            if (role == null || role.getName() == null || role.getName().isBlank()) return;
            roles.add(role.getName().trim());
        });
        return roles;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
