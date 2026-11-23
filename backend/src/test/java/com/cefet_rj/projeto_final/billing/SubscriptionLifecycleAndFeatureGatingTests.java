package com.cefet_rj.projeto_final.billing;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.events.SubscriptionUpdatedEvent;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class SubscriptionLifecycleAndFeatureGatingTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ApplicationEvents applicationEvents;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;

    private Condominium condo;
    private User adminUser;
    private User managerUser;
    private User residentUser;
    private BillingPlan essencialPlan;
    private BillingPlan profissionalPlan;
    private BillingPlan enterprisePlan;

    @BeforeEach
    void setUp() {
        Role adminRole = ensureRole("ROLE_ADMIN");
        Role managerRole = ensureRole("ROLE_MANAGER");
        Role residentRole = ensureRole("ROLE_RESIDENT");

        essencialPlan = ensurePlanWithCodes("ESSENCIAL_STAGE3_TEST", "199.90", Set.of(
                FeatureCode.PACKAGES, FeatureCode.ANNOUNCEMENTS, FeatureCode.NOTIFICATIONS,
                FeatureCode.COMMUNITY_TICKETS, FeatureCode.POLLS, FeatureCode.VISITORS
        ));
        profissionalPlan = ensurePlanWithCodes("PROFISSIONAL_STAGE3_TEST", "349.90", Set.of(
                FeatureCode.PACKAGES, FeatureCode.ANNOUNCEMENTS, FeatureCode.NOTIFICATIONS,
                FeatureCode.COMMUNITY_TICKETS, FeatureCode.POLLS, FeatureCode.VISITORS,
                FeatureCode.RESERVATIONS, FeatureCode.DOORMAN_CHAT, FeatureCode.FINANCIAL,
                FeatureCode.MAINTENANCE, FeatureCode.ASSEMBLIES, FeatureCode.EMERGENCY_SOS
        ));
        enterprisePlan = ensurePlanWithCodes("ENTERPRISE_STAGE3_TEST", "499.90", Set.of(FeatureCode.values()));

        String suffix = String.valueOf(System.nanoTime());
        adminUser = ensureUser("admin.sub." + suffix + "@test.com", "Admin", adminRole);
        managerUser = ensureUser("mgr.sub." + suffix + "@test.com", "Síndico", managerRole);
        residentUser = ensureUser("res.sub." + suffix + "@test.com", "Morador", residentRole);

        Condominium c = new Condominium();
        c.setName("Condomínio Assinatura Stage3 " + suffix);
        c.setAddress("Av. Central, 500");
        c.setBillingPlan(essencialPlan);
        c.setIsActive(true);
        c.setPlanSelected(true);
        condo = condominiumRepository.save(c);

        Membership mMgr = new Membership();
        mMgr.setCondominium(condo);
        mMgr.setUser(managerUser);
        mMgr.setRole(managerRole);
        mMgr.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mMgr);

        Membership mRes = new Membership();
        mRes.setCondominium(condo);
        mRes.setUser(residentUser);
        mRes.setRole(residentRole);
        mRes.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mRes);
    }

    @Test
    void subscriptionPlanGatingUpgradeSuspensionAndRenewalLifecycle() throws Exception {
        String managerToken = login(managerUser.getEmail());
        String residentToken = login(residentUser.getEmail());
        String adminToken = login(adminUser.getEmail());

        // 1) Essencial permite Tickets e Visitantes, mas bloqueia Financeiro, Manutenção, Lockers e IA com 403
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/financial/summary")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/maintenance/orders")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/assistant/query")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("question", "horário de silêncio"))))
                .andExpect(status().isForbidden());

        // 2) Consulta assinatura ativa pelo morador (retorna enabledFeatures do plano Essencial)
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/subscription")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessAllowed").value(true))
                .andExpect(jsonPath("$.billingPlanId").value(essencialPlan.getId()))
                .andExpect(jsonPath("$.enabledFeatures", hasItem("COMMUNITY_TICKETS")))
                .andExpect(jsonPath("$.enabledFeatures", not(hasItem("FINANCIAL"))));

        // 3) Não-admin é bloqueado ao tentar gerenciar assinaturas globais (/admin/subscriptions)
        mockMvc.perform(get("/admin/subscriptions")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 4) Upgrade de Essencial -> Profissional pelo síndico invalida cache e libera Financeiro/Manutenção, mantendo IA bloqueada
        mockMvc.perform(put("/manager/condominiums/" + condo.getId() + "/subscription/plan")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planId", profissionalPlan.getId(),
                                "billingCycle", "ANNUAL",
                                "reason", "Upgrade operacional do condomínio"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPlanId").value(profissionalPlan.getId()))
                .andExpect(jsonPath("$.billingCycle").value("ANNUAL"));

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/financial/summary")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/assistant/query")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("question", "horário de silêncio"))))
                .andExpect(status().isForbidden());

        // 5) Upgrade para Enterprise pelo Admin libera Assistente IA
        mockMvc.perform(put("/admin/subscriptions/" + condo.getId() + "/plan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planId", enterprisePlan.getId(),
                                "reason", "Ativação Enterprise"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPlanId").value(enterprisePlan.getId()));

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/assistant/query")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("question", "horário de silêncio"))))
                .andExpect(status().isOk());

        // 6) Suspensão da assinatura bloqueia Fail-Closed TODOS os módulos e zera enabledFeatures no payload
        mockMvc.perform(patch("/admin/subscriptions/" + condo.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "SUSPENDED",
                                "reason", "Inadimplência contratual"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.accessAllowed").value(false))
                .andExpect(jsonPath("$.enabledFeatures", hasSize(0)));

        mockMvc.perform(get("/condominiums/" + condo.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasSize(0)));

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // 7) Renovação reativa assinatura e restaura acesso imediatamente, registrando histórico completo
        mockMvc.perform(post("/admin/subscriptions/" + condo.getId() + "/renew")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("months", 3, "reason", "Fatura quitada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessAllowed").value(true));

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/subscription/history")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    void adminPlanMutationShouldPublishRealtimeSubscriptionUpdatedEvent() throws Exception {
        String adminToken = login(adminUser.getEmail());
        applicationEvents.clear();

        mockMvc.perform(put("/admin/subscriptions/" + condo.getId() + "/plan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planId", enterprisePlan.getId(),
                                "billingCycle", "ANNUAL",
                                "reason", "Teste SSE realtime"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPlanId").value(enterprisePlan.getId()));

        long publishedCount = applicationEvents.stream(SubscriptionUpdatedEvent.class)
                .filter(e -> e.condominiumId().equals(condo.getId()) && "ENTERPRISE_STAGE3_TEST".equals(e.planName()))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1L, publishedCount);
    }

    private Role ensureRole(String authority) {
        Role existing = roleRepository.findByAuthority(authority);
        if (existing != null) return existing;
        Role created = new Role();
        created.setAuthority(authority);
        return roleRepository.save(created);
    }

    private BillingPlan ensurePlanWithCodes(String name, String fee, Set<FeatureCode> codes) {
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
}
