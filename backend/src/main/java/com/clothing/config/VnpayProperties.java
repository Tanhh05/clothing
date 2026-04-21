package com.clothing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.vnpay")
public class VnpayProperties {

    private String endpoint = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String tmnCode = "";
    private String hashSecret = "";
    private String returnUrl = "http://localhost:5173/orders";
    private String ipnUrl = "http://localhost:8080/api/payments/vnpay/ipn";
    private String locale = "vn";
    private String orderType = "other";
    private String ipAddress = "127.0.0.1";
    private int expireMinutes = 15;
}
