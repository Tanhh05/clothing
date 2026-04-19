package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryUpsertRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name max length is 100")
    private String name;

    @Size(max = 255, message = "slug max length is 255")
    private String slug;

    @Size(max = 500, message = "imageUrl max length is 500")
    private String imageUrl;

    @Size(max = 150, message = "subtitle max length is 150")
    private String subtitle;

    @Size(max = 500, message = "externalLink max length is 500")
    private String externalLink;

    @Size(max = 50, message = "pageType max length is 50")
    private String pageType;

    @Size(max = 2000, message = "shortContent max length is 2000")
    private String shortContent;

    private Integer displayOrder;

    private Boolean showInMenu;

    @Size(max = 20, message = "status max length is 20")
    private String status;

    private Long parentId;
}
