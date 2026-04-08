package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderBulkStatusRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<Long> ids;

    @NotBlank(message = "status is required")
    private String status;
}

