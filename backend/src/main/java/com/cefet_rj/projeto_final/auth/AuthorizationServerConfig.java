package com.cefet_rj.projeto_final.auth;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.cefet_rj.projeto_final.auth.security.PersistentRsaKeyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import com.cefet_rj.projeto_final.auth.customgrant.CustomPasswordAuthenticationConverter;
import com.cefet_rj.projeto_final.auth.customgrant.CustomPasswordAuthenticationProvider;
import com.cefet_rj.projeto_final.auth.customgrant.CustomUserAuthorities;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

@Configuration
public class AuthorizationServerConfig {

    private static final AuthorizationGrantType PASSWORD_GRANT =
            new AuthorizationGrantType("password");

    @Value("${security.client-id}")
    private String clientId;

    @Value("${security.client-secret}")
    private String clientSecret;

    @Value("${security.jwt.duration}")
    private Integer jwtDurationSeconds;

    @Value("${security.jwt.refresh-duration}")
    private Integer refreshTokenDurationSeconds;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint
                                .accessTokenRequestConverter(
                                        new CustomPasswordAuthenticationConverter())
                                .authenticationProvider(
                                        new CustomPasswordAuthenticationProvider(
                                                authorizationService,
                                                tokenGenerator,
                                                userDetailsService,
                                                passwordEncoder
                                        )
                                )
                );

        http.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {

        return new JdbcOAuth2AuthorizationService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        JdbcRegisteredClientRepository repository =
                new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient existingClient =
                repository.findByClientId(clientId);

        if (existingClient != null) {
            return repository;
        }

        RegisteredClient client = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                )
                .authorizationGrantType(PASSWORD_GRANT)
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN
                )
                .scope("read")
                .scope("write")
                .tokenSettings(tokenSettings())
                .clientSettings(
                        ClientSettings.builder().build()
                )
                .build();

        repository.save(client);

        return repository;
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(
                        OAuth2TokenFormat.SELF_CONTAINED
                )
                .accessTokenTimeToLive(
                        Duration.ofSeconds(
                                jwtDurationSeconds
                        )
                )
                .refreshTokenTimeToLive(
                        Duration.ofSeconds(
                                refreshTokenDurationSeconds
                        )
                )
                .reuseRefreshTokens(false)
                .build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer) {

        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);

        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(tokenCustomizer);

        OAuth2AccessTokenGenerator accessTokenGenerator =
                new OAuth2AccessTokenGenerator();

        OAuth2RefreshTokenGenerator refreshTokenGenerator =
                new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                accessTokenGenerator,
                refreshTokenGenerator
        );
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {

        return context -> {

            if (!"access_token".equals(
                    context.getTokenType().getValue()
            )) {
                return;
            }

            Authentication principal =
                    context.getPrincipal();

            String username;
            List<String> authorities;

            if (principal instanceof OAuth2ClientAuthenticationToken clientPrincipal
                    && clientPrincipal.getDetails()
                    instanceof CustomUserAuthorities customUser) {

                username =
                        customUser.getUsername();

                authorities =
                        new ArrayList<>(
                                customUser.getAuthorities()
                                        .stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .toList()
                        );

            } else {

                username =
                        principal.getName();

                authorities =
                        new ArrayList<>(
                                principal.getAuthorities()
                                        .stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .toList()
                        );
            }

            context.getClaims()
                    .claim("authorities", authorities)
                    .claim("username", username);
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            PersistentRsaKeyService rsaKeyService
    ) {

        RSAKey rsaKey =
                rsaKeyService.loadOrCreate();

        JWKSet jwkSet =
                new JWKSet(rsaKey);

        return (selector, context) ->
                selector.select(jwkSet);
    }
}
