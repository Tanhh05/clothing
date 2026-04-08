package com.clothing.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductBulkActionRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<Long> ids;

    @Size(max = 20, message = "status max length is 20")
    private String status;
}

