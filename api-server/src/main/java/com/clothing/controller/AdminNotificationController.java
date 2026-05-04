package com.clothing.controller;

import com.clothing.dto.request.NotificationBroadcastRequest;
import com.clothing.dto.response.AdminNotificationResponse;
import com.clothing.service.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> getHistory() {
        return ResponseEntity.ok(adminNotificationService.getHistory());
    }

    @PostMapping
    public ResponseEntity<AdminNotificationResponse> create(@Valid @RequestBody NotificationBroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminNotificationService.create(request));
    }
}
