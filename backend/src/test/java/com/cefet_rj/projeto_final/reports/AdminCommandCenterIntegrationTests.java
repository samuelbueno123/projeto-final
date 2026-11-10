package com.cefet_rj.projeto_final.reports;

import com.cefet_rj.projeto_final.shared.observability.AuditLogService;
import com.cefet_rj.projeto_final.shared.observability.repositories.AuditLogRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCommandCenterIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

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
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String residentToken;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        User adminUser = getOrCreateUser("Admin", "CommandCenter", "admin.commandcenter@system.com", adminRole);
        User residentUser = getOrCreateUser("Resident", "CommandCenter", "resident.commandcenter@system.com", residentRole);

        adminToken = login(adminUser.getEmail());
        residentToken = login(residentUser.getEmail());
    }

    private Role getOrCreateRole(String authority) {
        Role r = roleRepository.findByAuthority(authority);
        if (r == null || r.getId() == null || !roleRepository.existsById(r.getId())) {
            r = new Role();
            r.setAuthority(authority);
            r = roleRepository.save(r);
        }
        return r;
    }

    private User getOrCreateUser(String firstName, String lastName, String email, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            u.addRole(role);
            return userRepository.save(u);
        });
    }

    private String login(String email) throws Exception {
        String credentials = "test-client:test-secret";
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String response = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basicAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", email)
                        .param("password", DEFAULT_PASSWORD)
                        .param("scope", "read write"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("access_token").asText();
    }

    @Test
    @DisplayName("GET /admin/overview returns comprehensive real statistics, role distribution, and audit summaries")
    void overviewShouldReturnAggregatedAdminStatistics() throws Exception {
        mockMvc.perform(get("/admin/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCondominiums").isNumber())
                .andExpect(jsonPath("$.inactiveCondominiums").isNumber())
                .andExpect(jsonPath("$.usersByRole.ROLE_ADMIN").isNumber())
                .andExpect(jsonPath("$.usersByRole.ROLE_RESIDENT").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.users").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.condominiums").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.packages").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.packageHistory").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.assemblies").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.maintenanceOrders").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.expenses").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.emergencyAlerts").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.shiftLogs").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.renovationRequests").isNumber())
                .andExpect(jsonPath("$.moduleVolumes.auditLogs").isNumber())
                .andExpect(jsonPath("$.recentAuditLogs").isArray());
    }

    @Test
    @DisplayName("GET /admin/audit-logs supports filtering by category, severity, outcome, principal, and free-text query")
    void auditLogsShouldSupportFilteringAndPagination() throws Exception {
        auditLogService.logAuthentication("audit-filter-user@test.com", true, "192.168.10.25", "PASSWORD_GRANT_OK");
        auditLogService.logSecurityEvent("UNAUTHORIZED_ENDPOINT", "intruder@test.com", "GET /admin/overview", "/admin/overview", false, "Tentativa bloqueada");
        auditLogService.logEmergencyAlert("porteiro@test.com", 999L, "MEDICAL", 1L);

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("category", "AUTH")
                        .param("outcome", "SUCCESS")
                        .param("principal", "audit-filter-user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].principal").value("audit-filter-user@test.com"))
                .andExpect(jsonPath("$.content[0].category").value("AUTH"))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("severity", "CRITICAL")
                        .param("category", "EMERGENCY")
                        .param("condominiumId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"));

        mockMvc.perform(get("/admin/audit-logs/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.byCategory").isMap())
                .andExpect(jsonPath("$.bySeverity").isMap())
                .andExpect(jsonPath("$.byOutcome").isMap());
    }

    @Test
    @DisplayName("Audit logs are strictly immutable via HTTP (DELETE and PUT are rejected)")
    void auditLogsShouldBeImmutable() throws Exception {
        mockMvc.perform(delete("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("GET /admin/diagnostics and POST /admin/diagnostics/cache/evict work and record an audit log")
    void diagnosticsAndCacheEvictionShouldWorkAndRecordAuditLog() throws Exception {
        mockMvc.perform(get("/admin/diagnostics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("UP"))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.jvm.heapUsedMb").isNumber())
                .andExpect(jsonPath("$.disk.freeMb").isNumber())
                .andExpect(jsonPath("$.realtime.activeConnections").isNumber())
                .andExpect(jsonPath("$.caches").isArray())
                .andExpect(jsonPath("$.operationalMetrics.loginSuccess").isNumber());

        long countBefore = auditLogRepository.count();

        mockMvc.perform(post("/admin/diagnostics/cache/evict")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.clearedCaches").isArray());

        assertThat(auditLogRepository.count()).isGreaterThan(countBefore);
    }

    @Test
    @DisplayName("GET /admin/permissions-matrix and newly exposed /admin/data/{resource} endpoints return valid responses")
    void permissionsMatrixAndNewAdminDataResourcesShouldReturnOk() throws Exception {
        mockMvc.perform(get("/admin/permissions-matrix")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(4))
                .andExpect(jsonPath("$.billingPlans").isArray())
                .andExpect(jsonPath("$.features").isArray());

        String[] newResources = {
                "package-history",
                "assemblies",
                "maintenance-orders",
                "expenses",
                "emergency-alerts",
                "shift-logs",
                "renovation-requests",
                "key-registry",
                "utility-readings",
                "parking-spots",
                "polls",
                "audit-logs"
        };

        for (String resource : newResources) {
            mockMvc.perform(get("/admin/data/" + resource)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Test
    @DisplayName("Non-admin users receive 403 Forbidden on all Admin Command Center endpoints and generate ACCESS_DENIED audit event")
    void nonAdminShouldBeDeniedAndLogged() throws Exception {
        long countBefore = auditLogRepository.count();

        mockMvc.perform(get("/admin/overview")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/diagnostics")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/diagnostics/cache/evict")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        assertThat(auditLogRepository.count()).isGreaterThan(countBefore);
    }
}
