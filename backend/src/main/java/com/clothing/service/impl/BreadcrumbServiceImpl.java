package com.clothing.service.impl;

import com.clothing.dto.response.BreadcrumbItemResponse;
import com.clothing.entity.CategoryEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.CategoryRepository;
import com.clothing.service.BreadcrumbService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BreadcrumbServiceImpl implements BreadcrumbService {

    private final CategoryRepository categoryRepository;

    public BreadcrumbServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<BreadcrumbItemResponse> getBreadcrumbBySlug(String slug) {
        List<BreadcrumbItemResponse> items = new ArrayList<>();
        items.add(new BreadcrumbItemResponse("Home", ""));

        if (slug == null || slug.isBlank()) {
            return items;
        }

        CategoryEntity category = categoryRepository.findBySlugIgnoreCase(slug.trim())
                .orElseThrow(() -> new BusinessException("Category not found for slug: " + slug, HttpStatus.NOT_FOUND));

        List<BreadcrumbItemResponse> categoryPath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        CategoryEntity current = category;
        while (current != null) {
            if (current.getId() != null && !visited.add(current.getId())) {
                break;
            }
            categoryPath.add(new BreadcrumbItemResponse(
                    current.getName(),
                    current.getSlug() == null ? toSlug(current.getName()) : current.getSlug()
            ));
            current = current.getParent();
        }

        Collections.reverse(categoryPath);
        items.addAll(categoryPath);
        return items;
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
