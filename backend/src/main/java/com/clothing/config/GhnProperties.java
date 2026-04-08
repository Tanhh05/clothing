package com.clothing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ghn")
public class GhnProperties {

    private String baseUrl = "https://online-gateway.ghn.vn/shiip/public-api/master-data";
    private String token = "";
}
