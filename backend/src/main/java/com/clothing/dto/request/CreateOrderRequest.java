package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 50, message = "paymentMethod max length is 50")
    private String paymentMethod;

    @NotBlank(message = "address is required")
    private String address;

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

    @Size(max = 50, message = "voucherCode max length is 50")
    private String voucherCode;

    @Size(max = 50, message = "momoRequestType max length is 50")
    private String momoRequestType;

    @Size(max = 20, message = "vnpayBankCode max length is 20")
    private String vnpayBankCode;
}
