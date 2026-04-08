package com.clothing.service;

import com.clothing.dto.response.BreadcrumbItemResponse;

import java.util.List;

public interface BreadcrumbService {

    List<BreadcrumbItemResponse> getBreadcrumbBySlug(String slug);
}
