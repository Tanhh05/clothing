package com.clothing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PosCheckoutRequest {

    private Long customerId;

    @NotEmpty(message = "items is required")
    @Valid
    private List<PosCheckoutItemRequest> items;

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 50, message = "paymentMethod max length is 50")
    private String paymentMethod;

    @Size(max = 50, message = "voucherCode max length is 50")
    private String voucherCode;

    @Min(value = 0, message = "manualDiscount must be >= 0")
    private Long manualDiscount;

    @Min(value = 0, message = "shippingFee must be >= 0")
    private Long shippingFee;

    @Min(value = 0, message = "paidAmount must be >= 0")
    private Long paidAmount;

    private Boolean shipEnabled;

    @Size(max = 120, message = "recipientName max length is 120")
    private String recipientName;

    @Size(max = 30, message = "phone max length is 30")
    private String phone;

    @Size(max = 100, message = "province max length is 100")
    private String province;

    @Size(max = 120, message = "district max length is 120")
    private String district;

    @Size(max = 120, message = "ward max length is 120")
    private String ward;

    private String address;

    @Size(max = 2000, message = "note max length is 2000")
    private String note;
}
