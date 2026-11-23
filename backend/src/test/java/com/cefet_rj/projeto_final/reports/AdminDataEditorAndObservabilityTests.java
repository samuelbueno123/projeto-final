package com.cefet_rj.projeto_final.reports;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.billing.repositories.SubscriptionHistoryRepository;
import com.cefet_rj.projeto_final.billing.services.CondominiumSubscriptionService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.shared.observability.entities.AuditLog;
import com.cefet_rj.projeto_final.shared.observability.repositories.AuditLogRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@ActiveProfiles("test")
class AdminDataEditorAndObservabilityTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private CondominiumSubscriptionRepository subscriptionRepository;
    @Autowired private CondominiumSubscriptionService subscriptionService;
    @Autowired private SubscriptionHistoryRepository subscriptionHistoryRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private User adminUser;
    private User managerUser;
    private Condominium condo;
    private CondominiumSubscription subscription;
    private BillingPlan basicPlan;
    private BillingPlan enterprisePlan;

    @BeforeEach
    void setUp() {
        Role adminRole = ensureRole("ROLE_ADMIN");
        Role managerRole = ensureRole("ROLE_MANAGER");

        basicPlan = ensurePlan("ESSENCIAL_STAGE4_TEST", "199.00", Set.of(FeatureCode.PACKAGES, FeatureCode.COMMUNITY_TICKETS));
        enterprisePlan = ensurePlan("ENTERPRISE_STAGE4_TEST", "499.00", Set.of(FeatureCode.values()));

        String suffix = String.valueOf(System.nanoTime());
        adminUser = ensureUser("admin.data." + suffix + "@test.com", "AdminData", adminRole);
        managerUser = ensureUser("mgr.data." + suffix + "@test.com", "MgrData", managerRole);

        Condominium c = new Condominium();
        c.setName("Condomínio Editor Stage4 " + suffix);
        c.setAddress("Av. Gov, 100");
        c.setBillingPlan(basicPlan);
        c.setIsActive(true);
        c.setPlanSelected(true);
        condo = condominiumRepository.save(c);

        Membership mem = new Membership();
        mem.setCondominium(condo);
        mem.setUser(managerUser);
        mem.setRole(managerRole);
        mem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mem);

        subscription = subscriptionService.ensureSubscriptionExists(condo);
    }

    @Test
    void schemaProtectedFieldsPolicyReasonAndAuditDiffsAreEnforced() throws Exception {
        String adminToken = login(adminUser.getEmail());
        String managerToken = login(managerUser.getEmail());

        // 1) Não-admin é bloqueado em /admin/data/{resource}/schema
        mockMvc.perform(get("/admin/data/users/schema")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 2) Schema tipado retornado para users & condominium-subscriptions
        mockMvc.perform(get("/admin/data/users/schema")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("users"))
                .andExpect(jsonPath("$.requiresReason").value(true))
                .andExpect(jsonPath("$.protectedFields", hasItem("passwordHash")))
                .andExpect(jsonPath("$.fields[?(@.name == 'email')].type").value("EMAIL"));

        // 3) Policy bloqueia mutação de campos protegidos/imutáveis (googleSubject / id)
        mockMvc.perform(put("/admin/data/users/" + managerUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("googleSubject", "forged-sub"),
                                "reason", "Tentativa indevida"
                        ))))
                .andExpect(status().isBadRequest());

        // 4) Policy exige reason obrigatório para edição sensível
        mockMvc.perform(put("/admin/data/users/" + managerUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("firstName", "SemMotivo"),
                                "reason", "   "
                        ))))
                .andExpect(status().isBadRequest());

        // 5) Ledgers imutáveis (audit-logs, subscription-history) são 403 em PUT/DELETE
        mockMvc.perform(put("/admin/data/audit-logs/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("reason", "Alteração ilegal"),
                                "reason", "Teste"
                        ))))
                .andExpect(status().isForbidden());

        // 6) Edição estruturada de condominium-subscriptions delega para CondominiumSubscriptionService
        //    + gera SubscriptionHistory + registra AuditLog com beforeState/afterState JSON e reason!
        long historyBefore = subscriptionHistoryRepository.count();
        mockMvc.perform(put("/admin/data/condominium-subscriptions/" + subscription.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of(
                                        "billingPlanId", enterprisePlan.getId(),
                                        "status", "SUSPENDED",
                                        "billingCycle", "ANNUAL"
                                ),
                                "reason", "Auditoria contratual etapa 4"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("UPDATE"))
                .andExpect(jsonPath("$.reason").value("Auditoria contratual etapa 4"))
                .andExpect(jsonPath("$.beforeState.status").value("ACTIVE"))
                .andExpect(jsonPath("$.afterState.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.afterState.billingPlanId").value(enterprisePlan.getId()));

        assertThat(subscriptionHistoryRepository.count()).isGreaterThan(historyBefore);

        List<AuditLog> mutationLogs = auditLogRepository.findAll().stream()
                .filter(l -> "ADMIN_DATA_UPDATE_CONDOMINIUM_SUBSCRIPTIONS".equals(l.getAction()))
                .toList();
        assertThat(mutationLogs).isNotEmpty();
        AuditLog latest = mutationLogs.get(mutationLogs.size() - 1);
        assertThat(latest.getReason()).isEqualTo("Auditoria contratual etapa 4");
        assertThat(latest.getBeforeState()).contains("\"status\":\"ACTIVE\"");
        assertThat(latest.getAfterState()).contains("\"status\":\"SUSPENDED\"");

        // E Fail-Closed bloqueou imediatamente o condomínio suspenso
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void prometheusActuatorEndpointRequiresAdminAndExportsCustomCondoMetrics() throws Exception {
        String adminToken = login(adminUser.getEmail());
        String managerToken = login(managerUser.getEmail());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("condo_auth_login_success")));
    }

    private Role ensureRole(String authority) {
        Role existing = roleRepository.findByAuthority(authority);
        if (existing != null) return existing;
        Role r = new Role();
        r.setAuthority(authority);
        return roleRepository.save(r);
    }

    private BillingPlan ensurePlan(String name, String fee, Set<FeatureCode> codes) {
        return billingPlanRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            BillingPlan p = new BillingPlan();
            p.setName(name);
            p.setMonthlyFee(new BigDecimal(fee));
            p.setActive(true);
            Set<Feature> set = new HashSet<>();
            for (FeatureCode code : codes) {
                Feature f = featureRepository.findByCode(code.name()).orElseGet(() -> {
                    Feature nf = new Feature();
                    nf.setCode(code.name());
                    nf.setName(code.name());
                    return featureRepository.save(nf);
                });
                set.add(f);
            }
            p.setFeatures(set);
            return billingPlanRepository.save(p);
        });
    }

    private User ensureUser(String email, String firstName, Role role) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName("Teste");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        u.addRole(role);
        return userRepository.save(u);
    }

    private String login(String email) throws Exception {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString("test-client:test-secret".getBytes(StandardCharsets.UTF_8));
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
}
