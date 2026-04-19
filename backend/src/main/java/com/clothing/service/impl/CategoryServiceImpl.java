package com.clothing.service.impl;

import com.clothing.dto.request.CategoryUpsertRequest;
import com.clothing.dto.response.CategoryResponse;
import com.clothing.dto.response.PageResponse;
import com.clothing.entity.CategoryEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.CategoryRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryUpsertRequest request) {
        CategoryEntity parent = resolveParent(request.getParentId(), null);
        String slug = resolveSlug(request.getSlug(), request.getName(), null);

        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.getName().trim());
        entity.setSlug(slug);
        entity.setParent(parent);
        applyExtendedFields(entity, request);

        return toResponse(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryUpsertRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found", HttpStatus.NOT_FOUND));

        CategoryEntity parent = resolveParent(request.getParentId(), id);
        String slug = resolveSlug(request.getSlug(), request.getName(), id);

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setParent(parent);
        applyExtendedFields(category, request);

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found", HttpStatus.NOT_FOUND));

        if (categoryRepository.existsByParent_Id(id)) {
            throw new BusinessException("Category has child categories, cannot delete", HttpStatus.BAD_REQUEST);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("Category has products, cannot delete", HttpStatus.BAD_REQUEST);
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse getById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found", HttpStatus.NOT_FOUND));
        return toResponse(category);
    }

    @Override
    public PageResponse<CategoryResponse> getAll(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new BusinessException("page must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException("size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSortBy));

        Page<CategoryEntity> categoryPage = categoryRepository.findAll(pageable);
        return PageResponse.<CategoryResponse>builder()
                .content(categoryPage.getContent().stream().map(this::toResponse).toList())
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .first(categoryPage.isFirst())
                .last(categoryPage.isLast())
                .build();
    }

    private CategoryEntity resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException("Category parent cannot be itself", HttpStatus.BAD_REQUEST);
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException("Parent category not found", HttpStatus.BAD_REQUEST));
    }

    private String resolveSlug(String rawSlug, String name, Long currentId) {
        String slug = rawSlug == null || rawSlug.isBlank() ? toSlug(name) : toSlug(rawSlug);
        if (slug.isBlank()) {
            throw new BusinessException("Slug is invalid", HttpStatus.BAD_REQUEST);
        }
        boolean exists = categoryRepository.existsBySlugIgnoreCase(slug);
        if (!exists) {
            return slug;
        }
        if (currentId == null) {
            throw new BusinessException("Category slug already exists", HttpStatus.CONFLICT);
        }
        CategoryEntity existing = categoryRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new BusinessException("Category slug already exists", HttpStatus.CONFLICT));
        if (!existing.getId().equals(currentId)) {
            throw new BusinessException("Category slug already exists", HttpStatus.CONFLICT);
        }
        return slug;
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim()
                .replace('đ', 'd')
                .replace('Đ', 'D');

        String noAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noAccent
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }

    private void applyExtendedFields(CategoryEntity category, CategoryUpsertRequest request) {
        category.setImageUrl(normalizeNullable(request.getImageUrl()));
        category.setSubtitle(normalizeNullable(request.getSubtitle()));
        category.setExternalLink(normalizeNullable(request.getExternalLink()));
        category.setPageType(normalizePageType(request.getPageType()));
        category.setShortContent(normalizeNullable(request.getShortContent()));
        category.setDisplayOrder(request.getDisplayOrder() == null ? 0 : Math.max(0, request.getDisplayOrder()));
        category.setShowInMenu(Boolean.TRUE.equals(request.getShowInMenu()));
        category.setStatus(normalizeStatus(request.getStatus()));
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePageType(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return "TRANG_DON";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return "ACTIVE";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!upper.equals("ACTIVE") && !upper.equals("INACTIVE")) {
            throw new BusinessException("status must be ACTIVE or INACTIVE", HttpStatus.BAD_REQUEST);
        }
        return upper;
    }

    private CategoryResponse toResponse(CategoryEntity entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .imageUrl(entity.getImageUrl())
                .subtitle(entity.getSubtitle())
                .externalLink(entity.getExternalLink())
                .pageType(entity.getPageType())
                .shortContent(entity.getShortContent())
                .displayOrder(entity.getDisplayOrder())
                .showInMenu(entity.getShowInMenu())
                .status(entity.getStatus())
                .parentId(entity.getParent() == null ? null : entity.getParent().getId())
                .build();
    }
}
