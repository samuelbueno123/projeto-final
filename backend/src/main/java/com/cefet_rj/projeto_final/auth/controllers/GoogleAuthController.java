package com.cefet_rj.projeto_final.auth.controllers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cefet_rj.projeto_final.auth.security.GoogleIdentityTokenService;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GoogleAuthController {
    private final GoogleIdentityTokenService googleTokens;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JWKSource<SecurityContext> jwkSource;

    @Value("${security.jwt.duration}") private long tokenDurationSeconds;

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> signIn(@Valid @RequestBody GoogleLoginRequest request) {
        Jwt google;
        try {
            google = googleTokens.verify(request.idToken());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google inválido ou expirado.");
        }
        String email = google.getClaimAsString("email").trim().toLowerCase();
        String subject = google.getSubject();
        User user = users.findByGoogleSubject(subject)
                .or(() -> users.findByEmail(email))
                .orElseGet(() -> createGoogleUser(google, email));
        if (user.getGoogleSubject() == null) {
            user.setGoogleSubject(subject);
            user = users.save(user);
        }
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("projeto-final")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(tokenDurationSeconds))
                .subject(user.getEmail())
                .claim(JwtClaimNames.AUD, List.of("projeto-front"))
                .claim("username", user.getEmail())
                .claim("authorities", user.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .build();
        String token = new NimbusJwtEncoder(jwkSource).encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return ResponseEntity.ok(new GoogleLoginResponse(token, "Bearer", tokenDurationSeconds,
                user.getEmail(), user.getFirstName() + " " + user.getLastName()));
    }

    private User createGoogleUser(Jwt google, String email) {
        User user = new User();
        String givenName = google.getClaimAsString("given_name");
        String familyName = google.getClaimAsString("family_name");
        user.setFirstName(givenName == null || givenName.isBlank() ? email.substring(0, email.indexOf('@')) : givenName);
        user.setLastName(familyName == null ? "" : familyName);
        user.setEmail(email);
        user.setGoogleSubject(google.getSubject());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        Role resident = roles.findByAuthority("ROLE_RESIDENT");
        if (resident == null) throw new IllegalStateException("Role ROLE_RESIDENT is not configured");
        user.addRole(resident);
        return users.save(user);
    }

    public record GoogleLoginRequest(@NotBlank String idToken) {}
    public record GoogleLoginResponse(String accessToken, String tokenType, long expiresIn, String email, String name) {}
}
