package com.cefet_rj.projeto_final.security;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "security.client-id=test-client",
                "security.client-secret=test-secret"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleBasedAccessControlIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ResidentRepository residentRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condoAlpha;
    private Condominium condoBeta;
    private User adminUser;
    private User managerAlpha;
    private User residentAlpha;
    private User staffAlpha;
    private User managerBeta;

    @BeforeEach
    void setUp() {
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");
        Role staffRole = getOrCreateRole("ROLE_DOORMAN");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature announcementsFeature = getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        BillingPlan plan = new BillingPlan();
        plan.setName("RBAC_TEST_PLAN");
        plan.setMonthlyFee(new BigDecimal("199.90"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, announcementsFeature));
        plan = billingPlanRepository.save(plan);

        condoAlpha = new Condominium();
        condoAlpha.setName("Condomínio Alpha");
        condoAlpha.setAddress("Rua Alpha, 100");
        condoAlpha.setBillingPlan(plan);
        condoAlpha.setIsActive(true);
        condoAlpha = condominiumRepository.save(condoAlpha);

        condoBeta = new Condominium();
        condoBeta.setName("Condomínio Beta");
        condoBeta.setAddress("Rua Beta, 200");
        condoBeta.setBillingPlan(plan);
        condoBeta.setIsActive(true);
        condoBeta = condominiumRepository.save(condoBeta);

        Sector sectorAlpha = new Sector();
        sectorAlpha.setName("Bloco 1");
        sectorAlpha.setStructuralType(SectorStructuralType.BLOCK);
        sectorAlpha.setCondominium(condoAlpha);
        sectorAlpha = sectorRepository.save(sectorAlpha);

        Unit unitAlpha = new Unit();
        unitAlpha.setIdentifier("101");
        unitAlpha.setSector(sectorAlpha);
        unitAlpha = unitRepository.save(unitAlpha);

        // Users
        adminUser = createUser("Global", "Admin", "admin.rbac@system.com", adminRole);
        managerAlpha = createUser("Manager", "Alpha", "manager.alpha@system.com", managerRole);
        residentAlpha = createUser("Resident", "Alpha", "resident.alpha@system.com", residentRole);
        staffAlpha = createUser("Portaria", "Alpha", "staff.alpha@system.com", staffRole);
        managerBeta = createUser("Manager", "Beta", "manager.beta@system.com", managerRole);

        // Memberships Alpha
        assignMembership(managerAlpha, condoAlpha, managerRole);
        assignMembership(residentAlpha, condoAlpha, residentRole);
        assignMembership(staffAlpha, condoAlpha, staffRole);

        // Resident link
        Resident res = new Resident();
        res.setUser(residentAlpha);
        res.setUnit(unitAlpha);
        res.setType(ResidentType.TENANT);
        residentRepository.save(res);

        // Memberships Beta
        assignMembership(managerBeta, condoBeta, managerRole);
    }

    private void assignMembership(User user, Condominium condo, Role role) {
        Membership mem = new Membership();
        mem.setUser(user);
        mem.setCondominium(condo);
        mem.setRole(role);
        mem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mem);
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

    private Feature getOrCreateFeature(FeatureCode code) {
        return featureRepository.findByCode(code.name()).orElseGet(() -> {
            Feature f = new Feature();
            f.setCode(code.name());
            f.setName(code.name());
            return featureRepository.save(f);
        });
    }

    private User createUser(String firstName, String lastName, String email, Role role) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
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

    // =========================================================================
    // 1. UNAUTHENTICATED REQUESTS
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated requests to protected endpoints return 401 Unauthorized")
    void unauthenticatedRequests_return401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/sectors").param("condominiumId", condoAlpha.getId().toString()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/condominiums/" + condoAlpha.getId() + "/financial/expenses"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. PRIVILEGE ESCALATION PREVENTION (RESIDENT -> MANAGER ACTIONS)
    // =========================================================================

    @Test
    @DisplayName("Resident cannot create expenses (Manager only) -> 403 Forbidden")
    void residentCannotCreateExpenses() throws Exception {
        String token = login(residentAlpha.getEmail());

        mockMvc.perform(post("/condominiums/" + condoAlpha.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Manutenção não autorizada",
                                "category", "MAINTENANCE",
                                "amount", 500.0,
                                "dueDate", LocalDate.now().plusDays(10).toString()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident cannot create sectors or units -> 403 Forbidden")
    void residentCannotCreateSectors() throws Exception {
        String token = login(residentAlpha.getEmail());

        mockMvc.perform(post("/sectors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bloco Ilegal",
                                "condominiumId", condoAlpha.getId(),
                                "structuralType", "BLOCK"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident cannot approve membership applications -> 403 Forbidden")
    void residentCannotApproveMemberships() throws Exception {
        String token = login(residentAlpha.getEmail());

        mockMvc.perform(post("/manager/condominiums/" + condoAlpha.getId() + "/membership-requests/" + staffAlpha.getId() + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. CROSS-TENANT ISOLATION (CONDO ALPHA vs CONDO BETA)
    // =========================================================================

    @Test
    @DisplayName("Manager of Condo Alpha cannot access or modify resources in Condo Beta -> 403 Forbidden")
    void crossTenantManagerAccessDenied() throws Exception {
        String tokenAlpha = login(managerAlpha.getEmail());

        // Manager Alpha trying to list sectors of Condo Beta
        mockMvc.perform(get("/sectors")
                        .param("condominiumId", condoBeta.getId().toString())
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden());

        // Manager Alpha trying to view expenses of Condo Beta
        mockMvc.perform(get("/condominiums/" + condoBeta.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident of Condo Alpha cannot view assemblies of Condo Beta -> 403 Forbidden")
    void crossTenantResidentAccessDenied() throws Exception {
        String tokenAlpha = login(residentAlpha.getEmail());

        mockMvc.perform(get("/condominiums/" + condoBeta.getId() + "/assemblies")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. STAFF / DOORMAN PRIVILEGE BOUNDARIES
    // =========================================================================

    @Test
    @DisplayName("Staff (Doorman) can access visitor logs but cannot manage finances -> 403 Forbidden")
    void staffBoundariesEnforced() throws Exception {
        String tokenStaff = login(staffAlpha.getEmail());

        // Doorman trying to create expenses -> forbidden
        mockMvc.perform(post("/condominiums/" + condoAlpha.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Tentativa da portaria",
                                "category", "MAINTENANCE",
                                "amount", 100.0,
                                "dueDate", LocalDate.now().plusDays(5).toString()
                        ))))
                .andExpect(status().isForbidden());

        // Doorman can access visitor accesses
        mockMvc.perform(get("/condominiums/" + condoAlpha.getId() + "/visitors")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 5. ADMIN GLOBAL PRIVILEGE
    // =========================================================================

    @Test
    @DisplayName("Global Admin can access sectors in any condominium without explicit membership")
    void globalAdminCanAccessAnyCondominium() throws Exception {
        String tokenAdmin = login(adminUser.getEmail());

        mockMvc.perform(get("/sectors")
                        .param("condominiumId", condoAlpha.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sectors")
                        .param("condominiumId", condoBeta.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }
}
