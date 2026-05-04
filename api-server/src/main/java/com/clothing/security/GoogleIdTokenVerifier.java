package com.clothing.security;

import com.clothing.config.GoogleOAuthProperties;
import com.clothing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleIdTokenVerifier {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final JwtDecoder jwtDecoder;

    public GoogleIdTokenVerifier(GoogleOAuthProperties googleOAuthProperties) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(googleOAuthProperties.getJwkSetUri()).build();
    }

    public GoogleUserInfo verify(String idToken) {
        if (googleOAuthProperties.getClientId() == null || googleOAuthProperties.getClientId().isBlank()) {
            throw new BusinessException("Google OAuth is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            throw new BusinessException("Invalid Google id token", HttpStatus.UNAUTHORIZED);
        }

        validateIssuer(jwt);
        validateAudience(jwt);

        String email = jwt.getClaimAsString("email");
        String fullName = jwt.getClaimAsString("name");
        String subject = jwt.getSubject();
        String phoneNumber = resolvePhoneNumber(jwt);
        boolean emailVerified = parseEmailVerified(jwt.getClaim("email_verified"));

        if (email == null || email.isBlank()) {
            throw new BusinessException("Google token does not contain email", HttpStatus.UNAUTHORIZED);
        }
        if (!emailVerified) {
            throw new BusinessException("Google email is not verified", HttpStatus.UNAUTHORIZED);
        }
        if (subject == null || subject.isBlank()) {
            throw new BusinessException("Google token subject is invalid", HttpStatus.UNAUTHORIZED);
        }

        return new GoogleUserInfo(subject, email, fullName, phoneNumber);
    }

    private void validateIssuer(Jwt jwt) {
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        List<String> allowedIssuers = googleOAuthProperties.getAllowedIssuers();
        if (issuer == null || allowedIssuers == null || allowedIssuers.stream().noneMatch(issuer::equals)) {
            throw new BusinessException("Invalid Google token issuer", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateAudience(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        String clientId = googleOAuthProperties.getClientId();
        if (audiences == null || audiences.stream().noneMatch(clientId::equals)) {
            throw new BusinessException("Google token audience mismatch", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean parseEmailVerified(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String stringValue) {
            return "true".equalsIgnoreCase(stringValue);
        }
        return false;
    }

    private String resolvePhoneNumber(Jwt jwt) {
        String phoneNumber = jwt.getClaimAsString("phone_number");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = jwt.getClaimAsString("phone");
        }
        return phoneNumber == null ? null : phoneNumber.trim();
    }

    public record GoogleUserInfo(String sub, String email, String name, String phone) {
    }
}
