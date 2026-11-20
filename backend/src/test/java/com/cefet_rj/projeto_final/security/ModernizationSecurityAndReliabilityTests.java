package com.cefet_rj.projeto_final.security;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.services.AnnouncementService;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.*;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.*;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceEquipment;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceEquipmentRepository;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.reports.dtos.DashboardReportDTO;
import com.cefet_rj.projeto_final.reports.services.ReportService;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.users.services.PictureStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModernizationSecurityAndReliabilityTests {

    private static final String PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ResidentRepository residentRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private UnitPaymentRepository unitPaymentRepository;
    @Autowired private MaintenanceEquipmentRepository equipmentRepository;
    @Autowired private PictureStorageService pictureStorageService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private ReportService reportService;

    private Condominium condo;
    private Unit unit101;
    private Unit unit102;
    private User manager;
    private User resident1;
    private User resident2;
    private User unitlessResident;
    private String managerToken;
    private String resident1Token;
    private String resident2Token;
    private String unitlessToken;

    @BeforeEach
    void setUp() throws Exception {
        Role roleManager = ensureRole("ROLE_MANAGER");
        Role roleResident = ensureRole("ROLE_RESIDENT");
        BillingPlan fullPlan = ensureFullBillingPlan();

        condo = new Condominium();
        condo.setName("Condomínio Modernização " + System.nanoTime());
        condo.setAddress("Av. Central, 500");
        condo.setIsActive(true);
        condo.setPlanSelected(true);
        condo.setBillingPlan(fullPlan);
        condo = condominiumRepository.save(condo);

        Sector sector = new Sector();
        sector.setName("Torre A");
        sector.setStructuralType(SectorStructuralType.BLOCK);
        sector.setCondominium(condo);
        sector = sectorRepository.save(sector);

        unit101 = new Unit();
        unit101.setIdentifier("101");
        unit101.setSector(sector);
        unit101 = unitRepository.save(unit101);

        unit102 = new Unit();
        unit102.setIdentifier("102");
        unit102.setSector(sector);
        unit102 = unitRepository.save(unit102);

        String suffix = String.valueOf(System.nanoTime());
        manager = ensureUser("mgr_mod_" + suffix + "@test.com", "Gestor", "Mod", roleManager, null);
        resident1 = ensureUser("res1_mod_" + suffix + "@test.com", "Morador", "Um", roleResident, "https://lh3.googleusercontent.com/avatar1.png");
        resident2 = ensureUser("res2_mod_" + suffix + "@test.com", "Morador", "Dois", roleResident, null);
        unitlessResident = ensureUser("unitless_mod_" + suffix + "@test.com", "Morador", "SemUnidade", roleResident, null);

        ensureMembership(manager, condo, roleManager, null);
        ensureMembership(resident1, condo, roleResident, unit101);
        ensureMembership(resident2, condo, roleResident, unit102);
        ensureMembership(unitlessResident, condo, roleResident, null);

        ensureResident(resident1, unit101);
        ensureResident(resident2, unit102);

        managerToken = login(manager.getEmail());
        resident1Token = login(resident1.getEmail());
        resident2Token = login(resident2.getEmail());
        unitlessToken = login(unitlessResident.getEmail());
    }

    @Test
    void checkPlateRestrictedToStaffAndRequiresAtLeastThreeChars() throws Exception {
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/vehicles/check-plate")
                        .header("Authorization", "Bearer " + resident1Token)
                        .param("plate", "ABC1234"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/vehicles/check-plate")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("plate", "AB"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/vehicles/check-plate")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("plate", "ABC"))
                .andExpect(status().isOk());
    }

    @Test
    void smartLockerAllowsReuseAfterRetrievalAndBlocksDuplicateOccupied() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "unitId", unit101.getId(),
                "lockerName", "Armário Norte",
                "compartmentNumber", "12",
                "pinCode", "4321",
                "description", "Caixa Segura"
        ));

        String firstCreated = mockMvc.perform(post("/condominiums/" + condo.getId() + "/lockers")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long lockerId = objectMapper.readTree(firstCreated).get("id").asLong();

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/lockers")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/lockers/" + lockerId + "/retrieve")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinCode", "4321"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/lockers")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void unitlessResidentCannotBindForeignUnitOnTicketCreation() throws Exception {
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + unitlessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Tentativa indevida",
                                "description", "Teste de isolamento de unidade",
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void targetedAnnouncementMarkAsReadBlocksNonTargetResident() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager.getEmail(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))
        );
        var ann = announcementService.create(
                condo.getId(),
                new AnnouncementRequestDTO("Aviso Unidade 101", "Mensagem privada para 101", null, null, unit101.getId(), null, null)
        );
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/announcements/" + ann.id() + "/read")
                        .header("Authorization", "Bearer " + resident2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/announcements/" + ann.id() + "/read")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk());
    }

    @Test
    void validateVisitorQrCodeRejectsRequestedVisitorAndUserProfileValidationsHold() throws Exception {
        String visitorJson = mockMvc.perform(post("/condominiums/" + condo.getId() + "/visitors")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "visitorName", "Visitante Pendente",
                                "residentId", resident1.getId(),
                                "unitId", unit101.getId(),
                                "requestApproval", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accessCode = objectMapper.readTree(visitorJson).get("accessCode").asText();

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/visitors/validate-qr")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("accessCode", accessCode))))
                .andExpect(status().isConflict());

        // Preserves existing external Google HTTPS avatar on profile update without profileImage
        String updatedProfile = mockMvc.perform(put("/users/me/profile")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Morador",
                                "lastName", "Atualizado",
                                "email", resident1.getEmail()
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("https://lh3.googleusercontent.com/avatar1.png", objectMapper.readTree(updatedProfile).get("profileImage").asText());

        // Theme allow-list validation
        mockMvc.perform(patch("/users/me/theme")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("theme", "NEON_PINK"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/users/me/theme")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("theme", "dark"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themePreference").value("DARK"));

        // Magic-byte rejection on disguised base64 PNG
        String fakePng = "data:image/png;base64," + Base64.getEncoder().encodeToString("NOT_A_PNG_FILE".getBytes(StandardCharsets.UTF_8));
        assertThrows(ResponseStatusException.class,
                () -> pictureStorageService.resolveOrStoreProfileImage(resident1.getId(), fakePng, null));
        assertThrows(ResponseStatusException.class,
                () -> pictureStorageService.resolveOrStoreProfileImage(null, fakePng, null));
    }

    @Test
    void paidInvoiceBlocksConfirmationRequestAndReportCountsOperationalEquipment() throws Exception {
        UnitPayment payment = new UnitPayment();
        payment.setUnit(unit101);
        payment.setStatus(UnitPaymentStatus.PAID);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setDueDate(LocalDate.now());
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(manager);
        unitPaymentRepository.save(payment);

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/units/" + unit101.getId() + "/request-payment-confirmation")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("note", "Tentativa"))))
                .andExpect(status().isConflict());

        MaintenanceEquipment eq = new MaintenanceEquipment();
        eq.setCondominium(condo);
        eq.setName("Elevador Social 1");
        eq.setCategory("ELEVADORES");
        eq.setStatus("OPERATIONAL");
        eq.setRecurrenceDays(30);
        eq.setNextMaintenanceDate(LocalDate.now().minusDays(3));
        eq.setCreatedAt(LocalDateTime.now());
        equipmentRepository.save(eq);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager.getEmail(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))
        );
        try {
            DashboardReportDTO report = reportService.getDashboard(condo.getId());
            assertTrue(report.maintenance().overdueEquipment() >= 1);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Role ensureRole(String authority) {
        Role role = roleRepository.findByAuthority(authority);
        if (role != null) return role;
        Role created = new Role();
        created.setAuthority(authority);
        return roleRepository.save(created);
    }

    private BillingPlan ensureFullBillingPlan() {
        BillingPlan plan = billingPlanRepository.findByNameIgnoreCase("PLAN_MOD_TEST")
                .orElseGet(() -> {
                    BillingPlan bp = new BillingPlan();
                    bp.setName("PLAN_MOD_TEST");
                    bp.setMonthlyFee(new BigDecimal("299.90"));
                    bp.setActive(true);
                    return billingPlanRepository.save(bp);
                });
        for (FeatureCode code : FeatureCode.values()) {
            Feature feature = featureRepository.findByCode(code.name()).orElseGet(() -> {
                Feature f = new Feature();
                f.setCode(code.name());
                f.setName(code.name());
                f.setDescription(code.name());
                return featureRepository.save(f);
            });
            plan.getFeatures().add(feature);
        }
        return billingPlanRepository.save(plan);
    }

    private User ensureUser(String email, String firstName, String lastName, Role role, String profileImage) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setProfileImage(profileImage);
        u.addRole(role);
        return userRepository.save(u);
    }

    private void ensureMembership(User user, Condominium condo, Role role, Unit unit) {
        Membership m = new Membership();
        m.setUser(user);
        m.setCondominium(condo);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setRequestedUnit(unit);
        m.setCreatedAt(LocalDateTime.now());
        membershipRepository.save(m);
    }

    private void ensureResident(User user, Unit unit) {
        Resident r = new Resident();
        r.setUser(user);
        r.setUnit(unit);
        r.setType(ResidentType.OWNER);
        residentRepository.save(r);
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
        JsonNode json = objectMapper.readTree(response);
        return json.get("access_token").asText();
    }
}
