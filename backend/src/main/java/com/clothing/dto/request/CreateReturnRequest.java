package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateReturnRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotBlank(message = "returnType is required")
    @Size(max = 20, message = "returnType max length is 20")
    private String returnType;

    @NotBlank(message = "reasonCode is required")
    @Size(max = 50, message = "reasonCode max length is 50")
    private String reasonCode;

    @NotBlank(message = "reasonDetail is required")
    @Size(min = 10, max = 1000, message = "reasonDetail length must be between 10 and 1000")
    private String reasonDetail;

    @Size(max = 2000, message = "evidenceUrls max length is 2000")
    private String evidenceUrls;

    @NotEmpty(message = "items are required")
    @Valid
    private List<CreateReturnRequestItem> items;
}
