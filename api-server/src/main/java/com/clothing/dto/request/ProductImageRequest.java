package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageRequest {

    @NotBlank(message = "url is required")
    @Size(max = 2000, message = "url max length is 2000")
    private String url;

    private Boolean isMain;
}
