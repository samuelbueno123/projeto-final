package com.cefet_rj.projeto_final.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityTokenService {
    private final JwtDecoder decoder;

    public GoogleIdentityTokenService(@Value("${security.google.client-id}") String clientId) {
        NimbusJwtDecoder googleDecoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer("https://accounts.google.com");
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Google token audience", null));
        googleDecoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult issuerResult = issuer.validate(jwt);
            if (issuerResult.hasErrors()) return issuerResult;
            return audience.validate(jwt);
        });
        this.decoder = googleDecoder;
    }

    public Jwt verify(String idToken) {
        Jwt jwt = decoder.decode(idToken);
        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified")) || jwt.getClaimAsString("email") == null) {
            throw new IllegalArgumentException("A conta Google precisa ter um email verificado.");
        }
        return jwt;
    }

    public Jwt verifyRecent(String idToken) {
        Jwt jwt = verify(idToken);
        if (jwt.getIssuedAt() == null || jwt.getIssuedAt().isBefore(java.time.Instant.now().minusSeconds(300))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Recent Google authentication is required", null));
        }
        return jwt;
    }
}
