package com.cefet_rj.projeto_final.auth.customgrant;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class CustomPasswordAuthenticationProvider implements AuthenticationProvider {

    private static final String ERROR_URI =
            "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    private static final AuthorizationGrantType PASSWORD_GRANT_TYPE =
            new AuthorizationGrantType("password");

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomPasswordAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        Assert.notNull(userDetailsService, "userDetailsService cannot be null");
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");

        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        CustomPasswordAuthenticationToken authRequest =
                (CustomPasswordAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(authRequest);

        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        assert registeredClient != null;
        if (!registeredClient.getAuthorizationGrantTypes().contains(PASSWORD_GRANT_TYPE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        String username = authRequest.getUsername();
        String password = authRequest.getPassword();

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            throw invalidGrant("Invalid credentials");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw invalidGrant("Invalid credentials");
        }

        Set<String> requestedScopes =
                authRequest.getScopes();

        Set<String> authorizedScopes;

        if (requestedScopes == null
                || requestedScopes.isEmpty()) {

            authorizedScopes =
                    new HashSet<>(
                            registeredClient.getScopes()
                    );

        } else {

            if (!registeredClient
                    .getScopes()
                    .containsAll(
                            requestedScopes
                    )) {

                throw new OAuth2AuthenticationException(
                        OAuth2ErrorCodes.INVALID_SCOPE
                );
            }

            authorizedScopes =
                    new HashSet<>(
                            requestedScopes
                    );
        }

        CustomUserAuthorities customUser =
                new CustomUserAuthorities(username, user.getAuthorities());

        var persistedAuthorities =
                new ArrayList<>(
                        user.getAuthorities()
                                .stream()
                                .map(authority ->
                                        new SimpleGrantedAuthority(
                                                authority.getAuthority()
                                        )
                                )
                                .toList()
                );

        Authentication userPrincipal =
                UsernamePasswordAuthenticationToken.authenticated(
                        username,
                        null,
                        persistedAuthorities
                );

        clientPrincipal.setDetails(customUser);

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(clientPrincipal);
        SecurityContextHolder.setContext(context);

        DefaultOAuth2TokenContext.Builder tokenContextBuilder =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(registeredClient)
                        .principal(clientPrincipal)
                        .authorizationServerContext(
                                AuthorizationServerContextHolder.getContext())
                        .authorizedScopes(authorizedScopes)
                        .authorizationGrantType(PASSWORD_GRANT_TYPE)
                        .authorizationGrant(authRequest);

        OAuth2Authorization.Builder authorizationBuilder =
                OAuth2Authorization.withRegisteredClient(registeredClient)
                        .principalName(username)
                        .authorizationGrantType(PASSWORD_GRANT_TYPE)
                        .authorizedScopes(authorizedScopes)
                        .attribute(Principal.class.getName(), userPrincipal);

        OAuth2TokenContext tokenContext =
                tokenContextBuilder
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .build();

        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);

        if (generatedToken == null) {
            OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.SERVER_ERROR,
                    "The token generator failed to generate the access token.",
                    ERROR_URI
            );
            throw new OAuth2AuthenticationException(error);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedToken.getTokenValue(),
                generatedToken.getIssuedAt(),
                generatedToken.getExpiresAt(),
                authorizedScopes
        );

        if (generatedToken instanceof ClaimAccessor accessor) {
            authorizationBuilder.token(
                    accessToken,
                    metadata -> metadata.put(
                            OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            accessor.getClaims()
                    )
            );
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        OAuth2TokenContext refreshTokenContext =
                tokenContextBuilder
                        .tokenType(
                                OAuth2TokenType.REFRESH_TOKEN
                        )
                        .build();

        OAuth2Token generatedRefreshToken =
                tokenGenerator.generate(
                        refreshTokenContext
                );

        if (!(generatedRefreshToken
                instanceof OAuth2RefreshToken refreshToken)) {

            OAuth2Error error =
                    new OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR,
                            "The token generator failed to generate the refresh token.",
                            ERROR_URI
                    );

            throw new OAuth2AuthenticationException(
                    error
            );
        }

        authorizationBuilder.refreshToken(
                refreshToken
        );

        OAuth2Authorization authorization =
                authorizationBuilder.build();

        authorizationService.save(
                authorization
        );

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2ClientAuthenticationToken
    getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {

        if (authentication.getPrincipal()
                instanceof OAuth2ClientAuthenticationToken clientPrincipal
                && clientPrincipal.isAuthenticated()) {

            return clientPrincipal;
        }

        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    private static OAuth2AuthenticationException invalidGrant(String message) {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_GRANT,
                message,
                ERROR_URI
        );
        return new OAuth2AuthenticationException(error);
    }
}