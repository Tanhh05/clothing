package com.clothing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BreadcrumbItemResponse {

    private String name;
    private String slug;
}
