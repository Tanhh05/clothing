package com.clothing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductUpsertRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name max length is 255")
    private String name;

    @Size(max = 255, message = "slug max length is 255")
    private String slug;

    private String description;

    @Size(max = 100, message = "brand max length is 100")
    private String brand;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @Size(max = 20, message = "status max length is 20")
    private String status;

    @Valid
    @NotEmpty(message = "variants must not be empty")
    private List<ProductVariantRequest> variants;

    @Valid
    private List<ProductImageRequest> images;
}
