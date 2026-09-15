package com.cefet_rj.projeto_final.auth;

import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "security.client-id=test-client",
                "security.client-secret=test-secret"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void passwordGrantShouldIssueRefreshTokenAndRefreshShouldPreserveAuthorities()
            throws Exception {

        String email =
                "auth-flow@test.com";

        String rawPassword =
                "StrongPassword123!";

        Role adminRole =
                roleRepository.findByAuthority(
                        "ROLE_ADMIN"
                );

        if (adminRole == null) {

            adminRole = new Role();

            adminRole.setAuthority(
                    "ROLE_ADMIN"
            );

            adminRole =
                    roleRepository.save(
                            adminRole
                    );
        }

        User user =
                new User();

        user.setFirstName(
                "Auth"
        );

        user.setLastName(
                "Tester"
        );

        user.setEmail(
                email
        );

        user.setPassword(
                passwordEncoder.encode(
                        rawPassword
                )
        );

        user.addRole(
                adminRole
        );

        userRepository.save(
                user
        );

        String basicAuthorization =
                basicAuthorization(
                        "test-client",
                        "test-secret"
                );

        String loginResponse =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .header(
                                                "Authorization",
                                                basicAuthorization
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_FORM_URLENCODED
                                        )
                                        .param(
                                                "grant_type",
                                                "password"
                                        )
                                        .param(
                                                "username",
                                                email
                                        )
                                        .param(
                                                "password",
                                                rawPassword
                                        )
                                        .param(
                                                "scope",
                                                "read"
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode loginJson =
                objectMapper.readTree(
                        loginResponse
                );

        String accessToken =
                loginJson
                        .get("access_token")
                        .asText();

        String refreshToken =
                loginJson
                        .get("refresh_token")
                        .asText();

        assertNotNull(
                accessToken
        );

        assertFalse(
                accessToken.isBlank()
        );

        assertNotNull(
                refreshToken
        );

        assertFalse(
                refreshToken.isBlank()
        );

        Jwt firstJwt =
                jwtDecoder.decode(
                        accessToken
                );

        assertEquals(
                email,
                firstJwt.getClaimAsString(
                        "username"
                )
        );

        List<String> firstAuthorities =
                firstJwt.getClaimAsStringList(
                        "authorities"
                );

        assertNotNull(
                firstAuthorities
        );

        assertTrue(
                firstAuthorities.contains(
                        "ROLE_ADMIN"
                )
        );

        String refreshResponse =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .header(
                                                "Authorization",
                                                basicAuthorization
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_FORM_URLENCODED
                                        )
                                        .param(
                                                "grant_type",
                                                "refresh_token"
                                        )
                                        .param(
                                                "refresh_token",
                                                refreshToken
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode refreshJson =
                objectMapper.readTree(
                        refreshResponse
                );

        String refreshedAccessToken =
                refreshJson
                        .get("access_token")
                        .asText();

        String refreshedRefreshToken =
                refreshJson
                        .get("refresh_token")
                        .asText();

        assertNotNull(
                refreshedAccessToken
        );

        assertFalse(
                refreshedAccessToken.isBlank()
        );

        assertNotNull(
                refreshedRefreshToken
        );

        assertFalse(
                refreshedRefreshToken.isBlank()
        );

        assertNotEquals(
                refreshToken,
                refreshedRefreshToken
        );

        Jwt refreshedJwt =
                jwtDecoder.decode(
                        refreshedAccessToken
                );

        assertEquals(
                email,
                refreshedJwt.getClaimAsString(
                        "username"
                )
        );

        List<String> refreshedAuthorities =
                refreshedJwt.getClaimAsStringList(
                        "authorities"
                );

        assertNotNull(
                refreshedAuthorities
        );

        assertTrue(
                refreshedAuthorities.contains(
                        "ROLE_ADMIN"
                )
        );
    }

    private String basicAuthorization(
            String clientId,
            String clientSecret
    ) {

        String credentials =
                clientId
                        + ":"
                        + clientSecret;

        String encoded =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        return "Basic " + encoded;
    }
}