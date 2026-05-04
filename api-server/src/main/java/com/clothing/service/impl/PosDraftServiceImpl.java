package com.clothing.service.impl;

import com.clothing.dto.request.PosDraftUpsertRequest;
import com.clothing.dto.response.PosDraftResponse;
import com.clothing.entity.PosDraftEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.PosDraftRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.PosDraftService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PosDraftServiceImpl implements PosDraftService {

    private final UserRepository userRepository;
    private final PosDraftRepository posDraftRepository;
    private final ObjectMapper objectMapper;

    public PosDraftServiceImpl(
            UserRepository userRepository,
            PosDraftRepository posDraftRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.posDraftRepository = posDraftRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PosDraftResponse saveDraft(String adminUsername, PosDraftUpsertRequest request) {
        UserEntity admin = getUser(adminUsername);
        String terminalId = safeTrim(request.getTerminalId());
        if (terminalId.isBlank()) {
            throw new BusinessException("terminalId is required", HttpStatus.BAD_REQUEST);
        }

        PosDraftEntity entity = posDraftRepository
                .findByAdminUserIdAndTerminalId(admin.getId(), terminalId)
                .orElseGet(PosDraftEntity::new);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setAdminUserId(admin.getId());
            entity.setTerminalId(terminalId);
            entity.setCreatedAt(now);
        }
        entity.setPayload(writeJson(request.getPayload()));
        entity.setUpdatedAt(now);
        PosDraftEntity saved = posDraftRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PosDraftResponse getDraft(String adminUsername, String terminalId) {
        UserEntity admin = getUser(adminUsername);
        String safeTerminalId = safeTrim(terminalId);
        if (safeTerminalId.isBlank()) {
            throw new BusinessException("terminalId is required", HttpStatus.BAD_REQUEST);
        }
        PosDraftEntity entity = posDraftRepository
                .findByAdminUserIdAndTerminalId(admin.getId(), safeTerminalId)
                .orElse(null);
        return entity == null ? null : toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteDraft(String adminUsername, String terminalId) {
        UserEntity admin = getUser(adminUsername);
        String safeTerminalId = safeTrim(terminalId);
        if (safeTerminalId.isBlank()) {
            throw new BusinessException("terminalId is required", HttpStatus.BAD_REQUEST);
        }
        posDraftRepository.deleteByAdminUserIdAndTerminalId(admin.getId(), safeTerminalId);
    }

    private PosDraftResponse toResponse(PosDraftEntity entity) {
        return PosDraftResponse.builder()
                .terminalId(entity.getTerminalId())
                .payload(readJson(entity.getPayload()))
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new BusinessException("Invalid draft payload", HttpStatus.BAD_REQUEST);
        }
    }

    private JsonNode readJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new BusinessException("Stored POS draft payload is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

