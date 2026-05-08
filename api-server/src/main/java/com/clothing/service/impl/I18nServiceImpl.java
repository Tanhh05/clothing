package com.clothing.service.impl;

import com.clothing.service.I18nService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class I18nServiceImpl implements I18nService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public I18nServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getMessages(String language) {
        String lang = normalizeLang(language);
        return cache.computeIfAbsent(lang, this::loadMessages);
    }

    private String normalizeLang(String language) {
        if (language == null || language.isBlank()) {
            return "vi";
        }
        String normalized = language.trim().toLowerCase();
        if (normalized.startsWith("en")) return "en";
        if (normalized.startsWith("my")) return "my";
        return "vi";
    }

    private Map<String, Object> loadMessages(String lang) {
        String path = "i18n/" + lang + ".json";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load i18n messages: " + path, ex);
        }
    }
}
