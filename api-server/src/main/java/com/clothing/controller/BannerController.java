package com.clothing.controller;

import com.clothing.dto.request.BannerUpsertRequest;
import com.clothing.dto.response.BannerResponse;
import com.clothing.service.BannerService;
import com.clothing.service.UploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BannerController {

    private final BannerService bannerService;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper;

    public BannerController(BannerService bannerService, UploadService uploadService, ObjectMapper objectMapper) {
        this.bannerService = bannerService;
        this.uploadService = uploadService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/banners")
    public ResponseEntity<List<BannerResponse>> getPublicBanners() {
        return ResponseEntity.ok(bannerService.getPublicBanners());
    }

    @GetMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping(value = "/admin/banners", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.create(request));
    }

    @PostMapping(value = "/admin/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> createMultipart(
            @RequestPart("data") String data,
            @RequestPart(value = "desktopFile", required = false) MultipartFile desktopFile,
            @RequestPart(value = "file", required = false) MultipartFile legacyFile
    ) throws Exception {
        BannerUpsertRequest request = objectMapper.readValue(data, BannerUpsertRequest.class);
        MultipartFile desktop = desktopFile != null ? desktopFile : legacyFile;
        if (desktop != null && !desktop.isEmpty()) {
            List<String> uploadedUrls = uploadService.uploadProductFiles(List.of(desktop));
            request.setImageUrl(uploadedUrls.get(0));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.create(request));
    }

    @PutMapping(value = "/admin/banners/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BannerUpsertRequest request
    ) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @PutMapping(value = "/admin/banners/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> updateMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "desktopFile", required = false) MultipartFile desktopFile,
            @RequestPart(value = "file", required = false) MultipartFile legacyFile
    ) throws Exception {
        BannerUpsertRequest request = objectMapper.readValue(data, BannerUpsertRequest.class);
        MultipartFile desktop = desktopFile != null ? desktopFile : legacyFile;
        if (desktop != null && !desktop.isEmpty()) {
            List<String> uploadedUrls = uploadService.uploadProductFiles(List.of(desktop));
            request.setImageUrl(uploadedUrls.get(0));
        }
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @DeleteMapping("/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
