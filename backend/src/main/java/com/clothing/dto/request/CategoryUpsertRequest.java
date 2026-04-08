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

    private Long parentId;
}
