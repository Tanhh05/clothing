package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSearchResponse {

    private Long id;
    private String name;
    private String slug;
    private String brand;
    private String categoryName;
    private String mainImageUrl;
    private Long minPrice;
    private Long maxPrice;
}
