package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressUpsertRequest {

    @NotBlank(message = "recipientName is required")
    @Size(max = 120, message = "recipientName max length is 120")
    private String recipientName;

    @NotBlank(message = "phone is required")
    @Size(max = 30, message = "phone max length is 30")
    private String phone;

    @NotBlank(message = "province is required")
    @Size(max = 120, message = "province max length is 120")
    private String province;

    @NotBlank(message = "district is required")
    @Size(max = 120, message = "district max length is 120")
    private String district;

    @NotBlank(message = "ward is required")
    @Size(max = 120, message = "ward max length is 120")
    private String ward;

    @NotBlank(message = "addressLine is required")
    private String addressLine;

    private Boolean isDefault;
}
