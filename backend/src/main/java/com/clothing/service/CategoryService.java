package com.clothing.service;

import com.clothing.dto.request.CategoryUpsertRequest;
import com.clothing.dto.response.CategoryResponse;
import com.clothing.dto.response.PageResponse;

public interface CategoryService {

    CategoryResponse create(CategoryUpsertRequest request);

    CategoryResponse update(Long id, CategoryUpsertRequest request);

    void delete(Long id);

    CategoryResponse getById(Long id);

    PageResponse<CategoryResponse> getAll(int page, int size, String sortBy, String direction);
}
