package com.cefet_rj.projeto_final.shared.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.cefet_rj.projeto_final.shared.exceptionhandler.StandardError;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductionObservabilityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationalMetricsService metricsService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String obtainAdminAccessToken() throws Exception {
        Role adminRole = roleRepository.findByAuthority("ROLE_ADMIN");
        if (adminRole == null || adminRole.getId() == null || !roleRepository.existsById(adminRole.getId())) {
            adminRole = new Role();
            adminRole.setAuthority("ROLE_ADMIN");
            adminRole = roleRepository.save(adminRole);
        }
        User admin = userRepository.findByEmailIgnoreCase("obs.admin@system.com").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setFirstName("Obs");
            admin.setLastName("Admin");
            admin.setEmail("obs.admin@system.com");
            admin.setPassword(passwordEncoder.encode("SecretPass123!"));
            admin.addRole(adminRole);
            userRepository.save(admin);
        }

        String credentials = "test-client:test-secret";
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String response = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basicAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", "obs.admin@system.com")
                        .param("password", "SecretPass123!")
                        .param("scope", "read write"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("access_token").asText();
    }

    @Test
    @DisplayName("Actuator /actuator/health returns 200 UP with db, disk and memory status")
    void actuatorHealth_ShouldReturnUpWithComponents() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.jvmMemory.status").value("UP"))
                .andExpect(jsonPath("$.components.diskSpace.status").value("UP"))
                .andExpect(jsonPath("$.components.ping.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator /actuator/health/liveness and /readiness return 200 UP")
    void actuatorProbes_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator /actuator/info returns application metadata")
    void actuatorInfo_ShouldReturnApplicationMetadata() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").exists())
                .andExpect(jsonPath("$.app.version").value("1.0.0"));
    }

    @Test
    @DisplayName("Actuator /actuator/metrics requires ROLE_ADMIN and returns valid metrics when authenticated")
    void actuatorMetrics_ShouldReturnMetrics() throws Exception {
        metricsService.recordLoginSuccess();
        metricsService.recordReservationCreated();

        // Anonymous access should be rejected with 401
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        String adminToken = obtainAdminAccessToken();

        // Authenticated admin access should return 200 with metrics
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names[?(@ == 'condo.auth.login.success')]").exists());

        mockMvc.perform(get("/actuator/metrics/condo.auth.login.success")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("condo.auth.login.success"))
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
    }

    @Test
    @DisplayName("Correlation ID is generated and returned in header and error body when absent")
    void correlationId_WhenAbsent_ShouldBeGeneratedAndPropagated() throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        String responseCorrelationId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(responseCorrelationId).isNotBlank();

        StandardError error = objectMapper.readValue(result.getResponse().getContentAsString(), StandardError.class);
        assertThat(error.getCorrelationId()).isEqualTo(responseCorrelationId);
    }

    @Test
    @DisplayName("Correlation ID is preserved when passed in incoming request header")
    void correlationId_WhenProvided_ShouldBePreservedAndReturned() throws Exception {
        String customCorrelationId = "audit-prod-trace-987654";

        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customCorrelationId))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Correlation-ID")).isEqualTo(customCorrelationId);
    }

    @Test
    @DisplayName("SensitiveDataMaskingConverter masks passwords, tokens, and credentials in logs")
    void sensitiveDataMasking_ShouldMaskSensitiveData() {
        SensitiveDataMaskingConverter converter = new SensitiveDataMaskingConverter();
        converter.start();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger("TestLogger");

        String rawMessage = "User login failed for password=SuperSecret123 with token=eyJhbGciOiJIUzI1NiJ9.abc.xyz and Bearer eyJhbGciOiJIUzI1NiJ9.123.456 and card=4111 2222 3333 4444";
        LoggingEvent event = new LoggingEvent(
                "com.test.TestClass",
                logger,
                Level.INFO,
                rawMessage,
                null,
                null
        );

        String masked = converter.convert(event);

        assertThat(masked).doesNotContain("SuperSecret123");
        assertThat(masked).contains("password=\"***MASKED***\"");
        assertThat(masked).contains("token=\"***MASKED***\"");
        assertThat(masked).contains("Bearer ***MASKED_JWT***");
        assertThat(masked).contains("****-****-****-****");
    }

    @Test
    @DisplayName("AuditLogService logs security, auth, emergency and sensitive operations without throwing")
    void auditLogService_ShouldLogEventsSuccessfully() {
        auditLogService.logAuthentication("morador@condominio.com", true, "192.168.1.100", "LOGIN_OK");
        auditLogService.logAuthentication("invasor@malicioso.com", false, "10.0.0.1", "BAD_CREDENTIALS");
        auditLogService.logRoleAssignment("admin@condominio.com", "porteiro@condominio.com", "ROLE_CONCIERGE", true);
        auditLogService.logEmergencyAlert("morador@condominio.com", 101L, "FIRE_ALARM", 1L);
        auditLogService.logSensitiveOperation("admin@condominio.com", "RESET_PASSWORD", "user-55", "Password reset initiated");
    }
}
