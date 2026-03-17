package com.cefet_rj.projeto_final.config.customgrant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

@Getter
public class CustomPasswordAuthenticationToken
        extends OAuth2AuthorizationGrantAuthenticationToken {

    private static final AuthorizationGrantType PASSWORD_GRANT_TYPE =
            new AuthorizationGrantType("password");

    private final String username;
    private final String password;
    private final Set<String> scopes;

    public CustomPasswordAuthenticationToken(
            Authentication clientPrincipal,
            @Nullable Set<String> scopes,
            @Nullable Map<String, Object> additionalParameters,
            String username,
            String password) {

        super(PASSWORD_GRANT_TYPE, clientPrincipal, additionalParameters);

        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        Assert.hasText(username, "username cannot be empty");
        Assert.hasText(password, "password cannot be empty");

        this.username = username;
        this.password = password;

        this.scopes = Collections.unmodifiableSet(
                scopes != null ? new HashSet<>(scopes) : Collections.emptySet()
        );
    }
}