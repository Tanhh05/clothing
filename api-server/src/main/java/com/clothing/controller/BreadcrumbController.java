package com.clothing.controller;

import com.clothing.dto.response.BreadcrumbItemResponse;
import com.clothing.service.BreadcrumbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/breadcrumbs")
public class BreadcrumbController {

    private final BreadcrumbService breadcrumbService;

    public BreadcrumbController(BreadcrumbService breadcrumbService) {
        this.breadcrumbService = breadcrumbService;
    }

    @GetMapping
    public ResponseEntity<List<BreadcrumbItemResponse>> getBreadcrumb(
            @RequestParam(name = "slug", required = false) String slug
    ) {
        return ResponseEntity.ok(breadcrumbService.getBreadcrumbBySlug(slug));
    }
}
