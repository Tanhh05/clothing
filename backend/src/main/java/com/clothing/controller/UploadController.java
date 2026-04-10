package com.clothing.controller;

import com.clothing.dto.request.UploadPresignRequest;
import com.clothing.dto.response.UploadPresignResponse;
import com.clothing.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping(value = "/review-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadReviewImages(
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(uploadService.uploadReviewFiles(files));
    }
}
