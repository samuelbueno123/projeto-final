package com.cefet_rj.projeto_final.community;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.community.entities.CondominiumInfraction;
import com.cefet_rj.projeto_final.community.entities.OccurrenceTicket;
import com.cefet_rj.projeto_final.community.entities.SmartLockerCompartment;
import com.cefet_rj.projeto_final.community.repositories.CondominiumInfractionRepository;
import com.cefet_rj.projeto_final.community.repositories.OccurrenceTicketRepository;
import com.cefet_rj.projeto_final.community.repositories.SmartLockerCompartmentRepository;
import com.cefet_rj.projeto_final.condominiums.entities.*;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
class SecurityAndHardeningIntegrationTests {

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
    @Autowired private OccurrenceTicketRepository ticketRepository;
    @Autowired private SmartLockerCompartmentRepository lockerRepository;
    @Autowired private CondominiumInfractionRepository infractionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condo;
    private Unit unit101;
    private User managerUser;
    private User residentUser;

    @BeforeEach
    void setUp() {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES);
        Feature announcementsFeature = getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        BillingPlan plan = new BillingPlan();
        plan.setName("TEST_SECURITY_PLAN");
        plan.setMonthlyFee(new BigDecimal("199.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, packagesFeature, announcementsFeature));
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Residencial Aurora Segura");
        condo.setAddress("Rua Teste, 100");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        Sector blockA = new Sector();
        blockA.setCondominium(condo);
        blockA.setName("Bloco A");
        blockA.setStructuralType(SectorStructuralType.BLOCK);
        blockA = sectorRepository.save(blockA);

        unit101 = new Unit();
        unit101.setSector(blockA);
        unit101.setIdentifier("101");
        unit101 = unitRepository.save(unit101);

        managerUser = createUser("Carlos", "Sindico", "carlos.manager.sec@test.com", managerRole);
        residentUser = createUser("Helena", "Moradora", "helena.resident.sec@test.com", residentRole);

        Membership mgrMem = new Membership();
        mgrMem.setUser(managerUser);
        mgrMem.setCondominium(condo);
        mgrMem.setRole(managerRole);
        mgrMem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mgrMem);

        Membership resMem = new Membership();
        resMem.setUser(residentUser);
        resMem.setCondominium(condo);
        resMem.setRole(residentRole);
        resMem.setStatus(MembershipStatus.ACTIVE);
        resMem.setRequestedUnit(unit101);
        membershipRepository.save(resMem);

        Resident resLink = new Resident();
        resLink.setUser(residentUser);
        resLink.setUnit(unit101);
        resLink.setType(ResidentType.OWNER);
        residentRepository.save(resLink);
    }

    @Test
    void testOccurrenceTicketNonStaffCannotForgeAdminResponseOrSetArbitraryStatus() throws Exception {
        String residentToken = login(residentUser.getEmail());

        OccurrenceTicket ticket = new OccurrenceTicket();
        ticket.setCondominium(condo);
        ticket.setAuthor(residentUser);
        ticket.setUnit(unit101);
        ticket.setCategory("MANUTENCAO");
        ticket.setTitle("Lampada queimada no corredor");
        ticket.setDescription("Trocar lampada");
        ticket.setStatus("OPEN");
        ticket = ticketRepository.save(ticket);

        // Resident tries to forge admin response and set RESOLVED
        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED",
                                "adminResponse", "Chamado resolvido pela administração falsa"
                        ))))
                .andExpect(status().isForbidden());

        // Resident cancels ticket without forging response -> Should succeed
        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CANCELLED"
                        ))))
                .andExpect(status().isOk());

        OccurrenceTicket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals("CANCELLED", updated.getStatus());
        assertNull(updated.getAdminResponse());
    }

    @Test
    void testSmartLockerRequiresPinForNonStaff() throws Exception {
        String residentToken = login(residentUser.getEmail());

        SmartLockerCompartment compartment = new SmartLockerCompartment();
        compartment.setCondominium(condo);
        compartment.setUnit(unit101);
        compartment.setRecipient(residentUser);
        compartment.setDepositedBy(managerUser);
        compartment.setCompartmentNumber("L-01");
        compartment.setPinCode("9876");
        compartment.setStatus("OCCUPIED");
        compartment = lockerRepository.save(compartment);

        // Resident tries to retrieve without PIN -> Must fail with 403 Forbidden
        mockMvc.perform(patch("/lockers/" + compartment.getId() + "/retrieve")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Resident tries with wrong PIN -> Must fail with 403 Forbidden
        mockMvc.perform(patch("/lockers/" + compartment.getId() + "/retrieve")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinCode", "0000"))))
                .andExpect(status().isForbidden());

        // Resident enters correct PIN -> Succeeds
        mockMvc.perform(patch("/lockers/" + compartment.getId() + "/retrieve")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinCode", "9876"))))
                .andExpect(status().isOk());

        SmartLockerCompartment updated = lockerRepository.findById(compartment.getId()).orElseThrow();
        assertEquals("RETRIEVED", updated.getStatus());
    }

    @Test
    void testInfractionDefenseRejectsExpiredDeadline() throws Exception {
        String residentToken = login(residentUser.getEmail());

        CondominiumInfraction infraction = new CondominiumInfraction();
        infraction.setCondominium(condo);
        infraction.setUnit(unit101);
        infraction.setIssuedBy(managerUser);
        infraction.setType("FINE");
        infraction.setTitle("Barulho excessivo");
        infraction.setDescription("Som alto após 22h");
        infraction.setStatus("ISSUED");
        infraction.setDefenseDeadline(LocalDate.now().minusDays(2)); // Expired!
        infraction = infractionRepository.save(infraction);

        mockMvc.perform(post("/infractions/" + infraction.getId() + "/defense")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "defense", "Não era som alto, era conversa baixa."
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnitListRequiresCondominiumIdForNonAdmin() throws Exception {
        String residentToken = login(residentUser.getEmail());
        String managerToken = login(managerUser.getEmail());

        // Without condominiumId, non-admin should get 403 Forbidden
        mockMvc.perform(get("/units")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/units")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // With condominiumId, manager should get 200 OK
        mockMvc.perform(get("/units?condominiumId=" + condo.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    private Role getOrCreateRole(String roleName) {
        Role role = roleRepository.findByAuthority(roleName);
        if (role == null || role.getId() == null || !roleRepository.existsById(role.getId())) {
            role = new Role();
            role.setAuthority(roleName);
            role = roleRepository.save(role);
        }
        return role;
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
}
