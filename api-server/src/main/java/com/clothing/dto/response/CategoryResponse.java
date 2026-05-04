package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private String subtitle;
    private String externalLink;
    private String pageType;
    private String shortContent;
    private Integer displayOrder;
    private Boolean showInMenu;
    private String status;
    private Long parentId;
}
