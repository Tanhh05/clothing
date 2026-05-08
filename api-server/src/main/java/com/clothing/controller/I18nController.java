package com.clothing.controller;

import com.clothing.config.RequestMetaResolver;
import com.clothing.service.I18nService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    private final I18nService i18nService;

    public I18nController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(HttpServletRequest request) {
        String language = RequestMetaResolver.resolveLanguage(request);
        return ResponseEntity.ok(i18nService.getMessages(language));
    }
}
