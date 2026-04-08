package com.clothing.controller;

import com.clothing.dto.request.UploadPresignRequest;
import com.clothing.dto.response.UploadPresignResponse;
import com.clothing.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<UploadPresignResponse> createPresignedUploadUrl(
            @Valid @RequestBody UploadPresignRequest request
    ) {
        return ResponseEntity.ok(uploadService.createPresignedUploadUrl(request));
    }
}
