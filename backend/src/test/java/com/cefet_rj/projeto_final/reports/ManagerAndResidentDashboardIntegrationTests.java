package com.cefet_rj.projeto_final.reports;

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
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerAndResidentDashboardIntegrationTests {

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
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private UnitPaymentRepository unitPaymentRepository;

    @Autowired
    private PackageRepository packageRepository;

    private String managerToken;
    private String residentToken;
    private Condominium condoA;
    private Condominium condoB;
    private Condominium condoOutsider;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES, "Encomendas");
        BillingPlan plan = getOrCreatePlan("DASH_TEST_PLAN_" + suffix, Set.of(packagesFeature));

        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        User manager = createUser("Sindico", "Dash", "sindico." + suffix + "@ancora.com", managerRole);
        User resident = createUser("Samuel", "Morador", "samuel." + suffix + "@ancora.com", residentRole);

        condoA = createCondo("Condominio Acqua " + suffix, plan);
        condoB = createCondo("Condominio Vista " + suffix, plan);
        condoOutsider = createCondo("Condominio Externo " + suffix, plan);

        Sector secA = createSector("Bloco 2", condoA);
        Sector secB = createSector("Torre Sul", condoB);
        Sector secOut = createSector("Bloco Unico", condoOutsider);

        Unit unitA1 = createUnit("1401", secA);
        Unit unitA2 = createUnit("1402", secA);
        Unit unitB1 = createUnit("201", secB);
        Unit unitOut1 = createUnit("99", secOut);

        linkMembership(manager, condoA, managerRole);
        linkMembership(resident, condoA, residentRole);
        linkMembership(resident, condoB, residentRole);

        Resident resLink = new Resident();
        resLink.setUser(resident);
        resLink.setUnit(unitA1);
        resLink.setType(ResidentType.OWNER);
        residentRepository.save(resLink);

        UnitPayment paidPayment = new UnitPayment();
        paidPayment.setUnit(unitA1);
        paidPayment.setStatus(UnitPaymentStatus.PAID);
        paidPayment.setAmount(new BigDecimal("450.00"));
        paidPayment.setDueDate(LocalDate.now());
        paidPayment.setUpdatedAt(LocalDateTime.now());
        paidPayment.setPaidAt(LocalDateTime.now());
        paidPayment.setUpdatedBy(manager);
        unitPaymentRepository.save(paidPayment);

        UnitPayment overduePayment = new UnitPayment();
        overduePayment.setUnit(unitA2);
        overduePayment.setStatus(UnitPaymentStatus.OVERDUE);
        overduePayment.setAmount(new BigDecimal("450.00"));
        overduePayment.setDueDate(LocalDate.now().minusDays(5));
        overduePayment.setUpdatedAt(LocalDateTime.now());
        overduePayment.setUpdatedBy(manager);
        unitPaymentRepository.save(overduePayment);

        createPackage("Notebook Acqua " + suffix, resident, unitA1);
        createPackage("Mercado Livre Vista " + suffix, resident, unitB1);
        createPackage("Pacote Externo " + suffix, resident, unitOut1);

        managerToken = login(manager.getEmail());
        residentToken = login(resident.getEmail());
    }

    @Test
    @DisplayName("GET /manager/dashboard returns real summary counts and enforces manager RBAC")
    void managerDashboardReturnsRealSummaryAndEnforcesRbac() throws Exception {
        mockMvc.perform(get("/manager/dashboard")
                        .param("condominiumId", condoA.getId().toString())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condominiumId").value(condoA.getId()))
                .andExpect(jsonPath("$.totalUnits").value(2))
                .andExpect(jsonPath("$.summary.occupiedUnits").value(1))
                .andExpect(jsonPath("$.summary.vacantUnits").value(1))
                .andExpect(jsonPath("$.summary.paidUnitsCount").value(1))
                .andExpect(jsonPath("$.summary.overdueUnitsCount").value(1))
                .andExpect(jsonPath("$.summary.totalCollected").value(450.0))
                .andExpect(jsonPath("$.summary.overdueAmount").value(450.0))
                .andExpect(jsonPath("$.summary.totalInvoiced").value(900.0));

        // Resident cannot access manager dashboard even within their own condo
        mockMvc.perform(get("/manager/dashboard")
                        .param("condominiumId", condoA.getId().toString())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // Manager cannot access condominium without membership
        mockMvc.perform(get("/manager/dashboard")
                        .param("condominiumId", condoOutsider.getId().toString())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /packages/my filters by condominiumId and blocks unauthorized condominiums")
    void myPackagesShouldFilterByCondominiumAndEnforceMembership() throws Exception {
        mockMvc.perform(get("/packages/my")
                        .param("condominiumId", condoA.getId().toString())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].condominiumId").value(condoA.getId()));

        mockMvc.perform(get("/packages/my")
                        .param("condominiumId", condoB.getId().toString())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].condominiumId").value(condoB.getId()));

        mockMvc.perform(get("/packages/my")
                        .param("condominiumId", condoOutsider.getId().toString())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    private Feature getOrCreateFeature(FeatureCode code, String name) {
        return featureRepository.findByCode(code.name()).orElseGet(() -> {
            Feature f = new Feature();
            f.setCode(code.name());
            f.setName(name);
            f.setDescription("Feature de teste: " + name);
            return featureRepository.save(f);
        });
    }

    private BillingPlan getOrCreatePlan(String name, Set<Feature> features) {
        BillingPlan plan = billingPlanRepository.findByNameIgnoreCase(name)
                .orElseGet(BillingPlan::new);
        plan.setName(name);
        plan.setMonthlyFee(new BigDecimal("199.90"));
        plan.setActive(true);
        plan.setFeatures(new HashSet<>(features));
        return billingPlanRepository.save(plan);
    }

    private Role getOrCreateRole(String authority) {
        Role existing = roleRepository.findByAuthority(authority);
        if (existing != null) {
            return existing;
        }
        Role role = new Role();
        role.setAuthority(authority);
        return roleRepository.save(role);
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

    private Condominium createCondo(String name, BillingPlan plan) {
        Condominium c = new Condominium();
        c.setName(name);
        c.setAddress("Av. Beira Mar, 100");
        c.setBillingPlan(plan);
        c.setIsActive(true);
        return condominiumRepository.save(c);
    }

    private Sector createSector(String name, Condominium condo) {
        Sector s = new Sector();
        s.setName(name);
        s.setStructuralType(SectorStructuralType.BLOCK);
        s.setCondominium(condo);
        return sectorRepository.save(s);
    }

    private Unit createUnit(String identifier, Sector sector) {
        Unit u = new Unit();
        u.setIdentifier(identifier);
        u.setSector(sector);
        return unitRepository.save(u);
    }

    private void linkMembership(User user, Condominium condo, Role role) {
        Membership m = new Membership();
        m.setUser(user);
        m.setCondominium(condo);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setHasSeenWelcome(true);
        membershipRepository.save(m);
    }

    private void createPackage(String description, User recipient, Unit unit) {
        Package p = new Package();
        p.setDescription(description);
        p.setRecipient(recipient);
        p.setUnit(unit);
        p.setStatus(PackageStatus.RECEIVED);
        p.setReceivedAt(LocalDateTime.now());
        packageRepository.save(p);
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
