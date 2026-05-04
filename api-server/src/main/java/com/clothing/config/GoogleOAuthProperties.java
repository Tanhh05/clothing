package com.clothing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.google")
public class GoogleOAuthProperties {

    private String clientId;
    private String jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";
    private List<String> allowedIssuers = List.of("https://accounts.google.com", "accounts.google.com");

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public List<String> getAllowedIssuers() {
        return allowedIssuers;
    }

    public void setAllowedIssuers(List<String> allowedIssuers) {
        this.allowedIssuers = allowedIssuers;
    }
}
