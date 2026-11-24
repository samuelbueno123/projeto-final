package com.cefet_rj.projeto_final.observability;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.observability.OperationalMetricsService;
import com.cefet_rj.projeto_final.shared.observability.SystemTelemetryService;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminTelemetryIntegrationTests {

    private static final String PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private SystemTelemetryService systemTelemetryService;
    @Autowired private OperationalMetricsService operationalMetricsService;

    private String adminToken;
    private String residentToken;
    private Condominium condo;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Role roleAdmin = ensureRole("ROLE_ADMIN");
        Role roleResident = ensureRole("ROLE_RESIDENT");

        User admin = ensureUser("admin_tel_" + suffix + "@test.com", "Admin", "Tel", roleAdmin);
        User resident = ensureUser("res_tel_" + suffix + "@test.com", "Morador", "Tel", roleResident);

        BillingPlan plan = billingPlanRepository.findByNameIgnoreCase("PLAN_TEL_TEST").orElseGet(() -> {
            BillingPlan bp = new BillingPlan();
            bp.setName("PLAN_TEL_TEST");
            bp.setMonthlyFee(new BigDecimal("199.90"));
            bp.setActive(true);
            return billingPlanRepository.save(bp);
        });

        condo = new Condominium();
        condo.setName("Condomínio Telemetria " + suffix);
        condo.setAddress("Av. Observabilidade, 100");
        condo.setIsActive(true);
        condo.setPlanSelected(true);
        condo.setBillingPlan(plan);
        condo = condominiumRepository.save(condo);

        adminToken = login(admin.getEmail());
        residentToken = login(resident.getEmail());
    }

    @Test
    void adminSnapshotReturnsRealInfrastructureHttpDatabaseAndSeries() throws Exception {
        String response = mockMvc.perform(get("/admin/telemetry/snapshot")
                        .param("window", "15m")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").exists())
                .andExpect(jsonPath("$.infrastructure.cpu.availableProcessors").isNumber())
                .andExpect(jsonPath("$.infrastructure.jvm.heapUsedMb").isNumber())
                .andExpect(jsonPath("$.infrastructure.disk.totalMb").isNumber())
                .andExpect(jsonPath("$.application.http.totalRequests").isNumber())
                .andExpect(jsonPath("$.application.http.latencyHistogram").isArray())
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.timeSeries").isArray())
                .andExpect(jsonPath("$.alertsSummary").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertTrue(root.get("timeSeries").size() >= 1);
        assertTrue(root.get("application").get("http").get("totalRequests").asLong() >= 1);
    }

    @Test
    void adminBusinessAnalyticsReturnsCondominiumsUsersModulesAndTimeline() throws Exception {
        String response = mockMvc.perform(get("/admin/telemetry/business")
                        .param("range", "7d")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("7d"))
                .andExpect(jsonPath("$.condominiums.total").isNumber())
                .andExpect(jsonPath("$.users.byRole").exists())
                .andExpect(jsonPath("$.moduleUsage").exists())
                .andExpect(jsonPath("$.condominiumRanking").isArray())
                .andExpect(jsonPath("$.activityTimeline").isArray())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertEquals(7, root.get("activityTimeline").size());
        assertTrue(root.get("condominiums").get("active").asLong() >= 1);
    }

    @Test
    void nonAdminCannotAccessTelemetryEndpoints() throws Exception {
        mockMvc.perform(get("/admin/telemetry/snapshot")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/telemetry/business")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/telemetry/alerts")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void alertAcknowledgeAndResolveLifecycleWorksAndIsAudited() throws Exception {
        // Dispara falha de login suficiente para gerar alerta real de anomalia se necessário
        for (int i = 0; i < 11; i++) {
            operationalMetricsService.recordLoginFailure("bad_credentials");
        }
        systemTelemetryService.collectScheduledSample();

        String alertsBody = mockMvc.perform(get("/admin/telemetry/alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode alertsJson = objectMapper.readTree(alertsBody);
        List<JsonNode> active = new java.util.ArrayList<>();
        alertsJson.get("active").forEach(active::add);
        assertFalse(active.isEmpty(), "Deveria haver ao menos um alerta ativo após 11 falhas de login");

        String alertId = active.get(0).get("id").asText();

        mockMvc.perform(post("/admin/telemetry/alerts/" + alertId + "/acknowledge")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        mockMvc.perform(post("/admin/telemetry/alerts/" + alertId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void realtimeEventsAndLegacySubscribeAliasAreAccessible() throws Exception {
        mockMvc.perform(get("/realtime/events")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/realtime/subscribe")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());
    }

    private Role ensureRole(String authority) {
        Role role = roleRepository.findByAuthority(authority);
        if (role != null) return role;
        Role created = new Role();
        created.setAuthority(authority);
        return roleRepository.save(created);
    }

    private User ensureUser(String email, String firstName, String lastName, Role role) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.addRole(role);
        return userRepository.save(u);
    }

    private String login(String email) throws Exception {
        String credentials = "test-client:test-secret";
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String response = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basicAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", email)
                        .param("password", PASSWORD)
                        .param("scope", "read write"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }
}
