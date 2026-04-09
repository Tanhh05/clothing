package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnStatusUpdateRequest {

    @NotBlank(message = "status is required")
    @Size(max = 20, message = "status max length is 20")
    private String status;

    @Size(max = 1000, message = "note max length is 1000")
    private String note;
}
