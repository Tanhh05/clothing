package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String status;
    private LocalDateTime createdAt;
    private Double ratingAvg;
    private Long reviewCount;
    private List<String> colors;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
}
