package com.clothing.controller;

import com.clothing.dto.request.UserAddressUpsertRequest;
import com.clothing.dto.response.UserAddressResponse;
import com.clothing.entity.UserAddressEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.UserAddressRepository;
import com.clothing.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    public UserAddressController(UserAddressRepository userAddressRepository, UserRepository userRepository) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserAddressResponse>> list(Authentication authentication) {
        UserEntity user = getUser(authentication.getName());
        List<UserAddressResponse> responses = userAddressRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/users/{userId}/default")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAddressResponse> getDefaultAddressByUserId(@PathVariable Long userId) {
        UserAddressEntity entity = userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> userAddressRepository.findByUserIdOrderByIdDesc(userId).stream().findFirst().orElse(null));
        if (entity == null) {
            throw new BusinessException("Address not found", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UserAddressResponse> create(
            Authentication authentication,
            @Valid @RequestBody UserAddressUpsertRequest request
    ) {
        UserEntity user = getUser(authentication.getName());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(user.getId());
        }

        UserAddressEntity entity = new UserAddressEntity();
        entity.setUserId(user.getId());
        entity.setRecipientName(request.getRecipientName().trim());
        entity.setPhone(request.getPhone().trim());
        entity.setProvince(request.getProvince().trim());
        entity.setDistrict(request.getDistrict().trim());
        entity.setWard(request.getWard().trim());
        entity.setAddressLine(request.getAddressLine().trim());
        entity.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        entity.setCreatedAt(LocalDateTime.now());
        UserAddressEntity saved = userAddressRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<UserAddressResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UserAddressUpsertRequest request
    ) {
        UserEntity user = getUser(authentication.getName());
        UserAddressEntity entity = userAddressRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Address not found", HttpStatus.NOT_FOUND));
        if (!user.getId().equals(entity.getUserId())) {
            throw new BusinessException("Not allowed", HttpStatus.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(user.getId());
        }
        entity.setRecipientName(request.getRecipientName().trim());
        entity.setPhone(request.getPhone().trim());
        entity.setProvince(request.getProvince().trim());
        entity.setDistrict(request.getDistrict().trim());
        entity.setWard(request.getWard().trim());
        entity.setAddressLine(request.getAddressLine().trim());
        entity.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        UserAddressEntity saved = userAddressRepository.save(entity);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        UserEntity user = getUser(authentication.getName());
        UserAddressEntity entity = userAddressRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Address not found", HttpStatus.NOT_FOUND));
        if (!user.getId().equals(entity.getUserId())) {
            throw new BusinessException("Not allowed", HttpStatus.FORBIDDEN);
        }
        userAddressRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    private void clearDefault(Long userId) {
        List<UserAddressEntity> addresses = userAddressRepository.findByUserIdOrderByIdDesc(userId);
        for (UserAddressEntity item : addresses) {
            if (Boolean.TRUE.equals(item.getIsDefault())) {
                item.setIsDefault(false);
            }
        }
        userAddressRepository.saveAll(addresses);
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private UserAddressResponse toResponse(UserAddressEntity entity) {
        return UserAddressResponse.builder()
                .id(entity.getId())
                .recipientName(entity.getRecipientName())
                .phone(entity.getPhone())
                .province(entity.getProvince())
                .district(entity.getDistrict())
                .ward(entity.getWard())
                .addressLine(entity.getAddressLine())
                .isDefault(Boolean.TRUE.equals(entity.getIsDefault()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
