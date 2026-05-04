package com.clothing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.momo")
public class MomoProperties {

    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String partnerCode = "";
    private String accessKey = "";
    private String secretKey = "";
    private String redirectUrl = "http://localhost:5173/orders";
    private String ipnUrl = "http://localhost:8080/api/payments/momo/ipn";
    private String requestType = "captureWallet";
}
