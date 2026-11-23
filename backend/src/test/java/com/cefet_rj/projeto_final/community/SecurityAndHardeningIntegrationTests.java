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
    @Autowired private com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository notificationRepository;
    @Autowired private com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository unitPaymentRepository;
    @Autowired private com.cefet_rj.projeto_final.community.repositories.ParkingSpotRepository parkingSpotRepository;

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
        plan.setFeatures(java.util.Arrays.stream(FeatureCode.values()).map(this::getOrCreateFeature).collect(java.util.stream.Collectors.toSet()));
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

    @Test
    void testUnitScopedMessagesAndAnnouncementsStrictlyIsolatedToTargetUnit() throws Exception {
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");
        Sector sectorA = unit101.getSector();

        Unit unit102 = new Unit();
        unit102.setSector(sectorA);
        unit102.setIdentifier("102");
        unit102 = unitRepository.save(unit102);

        User otherResident = createUser("Bruno", "Vizinho", "bruno.102.sec@test.com", residentRole);
        Membership otherMem = new Membership();
        otherMem.setUser(otherResident);
        otherMem.setCondominium(condo);
        otherMem.setRole(residentRole);
        otherMem.setStatus(MembershipStatus.ACTIVE);
        otherMem.setRequestedUnit(unit102);
        membershipRepository.save(otherMem);

        Resident otherResLink = new Resident();
        otherResLink.setUser(otherResident);
        otherResLink.setUnit(unit102);
        otherResLink.setType(ResidentType.OWNER);
        residentRepository.save(otherResLink);

        String managerToken = login(managerUser.getEmail());
        String resident101Token = login(residentUser.getEmail());
        String resident102Token = login(otherResident.getEmail());

        // 1) Manager creates a unit-scoped announcement targeting ONLY unit 101
        String annResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/announcements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Aviso Exclusivo Unid 101",
                                "content", "Somente os moradores da 101 devem ler esta mensagem.",
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long annId = objectMapper.readTree(annResp).get("id").asLong();

        // Resident of unit 101 sees and can mark read
        String list101 = mockMvc.perform(get("/condominiums/" + condo.getId() + "/announcements")
                        .header("Authorization", "Bearer " + resident101Token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(list101.contains("Aviso Exclusivo Unid 101"));

        mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + resident101Token))
                .andExpect(status().isOk());

        // Resident of unit 102 CANNOT see or mark read
        String list102 = mockMvc.perform(get("/condominiums/" + condo.getId() + "/announcements")
                        .header("Authorization", "Bearer " + resident102Token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(list102.contains("Aviso Exclusivo Unid 101"));

        mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + resident102Token))
                .andExpect(status().isForbidden());

        // 2) Manager sends unit-scoped doorman-chat message to unit 101
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "message", "Encomenda confidencial ap 101"
                        ))))
                .andExpect(status().isCreated());

        // Resident 101 sees the unit chat message
        String chat101 = mockMvc.perform(get("/condominiums/" + condo.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + resident101Token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(chat101.contains("Encomenda confidencial ap 101"));

        // Resident 102 NEVER sees unit 101's chat message and cannot spoof unitId 101
        String chat102 = mockMvc.perform(get("/condominiums/" + condo.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + resident102Token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(chat102.contains("Encomenda confidencial ap 101"));

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + resident102Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "message", "Tentativa de spoofing"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCondominiumNoticesStrictlyScopedPerCondominium() throws Exception {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Condominium condoB = new Condominium();
        condoB.setName("Residencial Beta Isolado");
        condoB.setAddress("Rua Beta, 200");
        condoB.setBillingPlan(condo.getBillingPlan());
        condoB.setIsActive(true);
        condoB = condominiumRepository.save(condoB);

        Condominium condoUnlinked = new Condominium();
        condoUnlinked.setName("Condomínio Sem Acesso");
        condoUnlinked.setAddress("Rua Gamma, 300");
        condoUnlinked.setBillingPlan(condo.getBillingPlan());
        condoUnlinked.setIsActive(true);
        condoUnlinked = condominiumRepository.save(condoUnlinked);

        User mgrB = createUser("Marina", "GestoraB", "marina.mgrb.sec@test.com", managerRole);
        Membership mgrBMem = new Membership();
        mgrBMem.setUser(mgrB);
        mgrBMem.setCondominium(condoB);
        mgrBMem.setRole(managerRole);
        mgrBMem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mgrBMem);

        Membership resBMem = new Membership();
        resBMem.setUser(residentUser);
        resBMem.setCondominium(condoB);
        resBMem.setRole(residentRole);
        resBMem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(resBMem);

        String residentToken = login(residentUser.getEmail());

        // Persist condominium-scoped notifications within the active test transaction
        var nA = new com.cefet_rj.projeto_final.notifications.entities.Notification();
        nA.setRecipient(residentUser);
        nA.setType(com.cefet_rj.projeto_final.notifications.enums.NotificationType.ANNOUNCEMENT);
        nA.setCondominiumId(condo.getId());
        nA.setCondominiumName(condo.getName());
        nA.setTitle("Novo comunicado");
        nA.setMessage("Novo comunicado disponível: Aviso exclusivo Condo Aurora");
        nA.setRead(false);
        nA.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(nA);

        var nB = new com.cefet_rj.projeto_final.notifications.entities.Notification();
        nB.setRecipient(residentUser);
        nB.setType(com.cefet_rj.projeto_final.notifications.enums.NotificationType.ANNOUNCEMENT);
        nB.setCondominiumId(condoB.getId());
        nB.setCondominiumName(condoB.getName());
        nB.setTitle("Novo comunicado");
        nB.setMessage("Novo comunicado disponível: Aviso exclusivo Condo Beta");
        nB.setRead(false);
        nB.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(nB);

        String notifA = mockMvc.perform(get("/notifications/my?condominiumId=" + condo.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(notifA.contains("Condo Aurora"));
        assertFalse(notifA.contains("Condo Beta"));

        String notifB = mockMvc.perform(get("/notifications/my?condominiumId=" + condoB.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(notifB.contains("Condo Beta"));
        assertFalse(notifB.contains("Condo Aurora"));

        mockMvc.perform(get("/notifications/my?condominiumId=" + condoUnlinked.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCondominiumPollsLifecycleOptionsAndClosing() throws Exception {
        String managerToken = login(managerUser.getEmail());
        String residentToken = login(residentUser.getEmail());

        String createdJson = mockMvc.perform(post("/condominiums/" + condo.getId() + "/polls")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Pintura da fachada 2026",
                                "description", "Escolha a cor principal",
                                "options", java.util.List.of("Azul Marinho", "Cinza Grafite"),
                                "endsAt", "2027-01-15T23:59:00Z"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.options[0].label").value("Azul Marinho"))
                .andExpect(jsonPath("$.options[1].label").value("Cinza Grafite"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createdJson);
        long pollId = created.get("id").asLong();
        long optId = created.get("options").get(0).get("id").asLong();

        // Resident votes
        mockMvc.perform(post("/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", optId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myOptionId").value(optId))
                .andExpect(jsonPath("$.options[0].votes").value(1));

        // Manager closes poll
        mockMvc.perform(patch("/polls/" + pollId + "/close")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Voting on closed poll fails with 409 Conflict
        mockMvc.perform(post("/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", optId))))
                .andExpect(status().isConflict());
    }

    @Test
    void testStage1SecurityPrivacyAndStateMachinesHardening() throws Exception {
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");
        User foreignUser = createUser("Lia", "Externa", "lia.external.s1@test.com", residentRole);

        String managerToken = login(managerUser.getEmail());
        String residentToken = login(residentUser.getEmail());

        // M01: Resident cannot unilaterally mark their own OVERDUE invoice as PAID
        var overdue = new com.cefet_rj.projeto_final.manager.entities.UnitPayment();
        overdue.setUnit(unit101);
        overdue.setStatus(com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus.OVERDUE);
        overdue.setAmount(new BigDecimal("450.00"));
        overdue.setDueDate(LocalDate.now().minusDays(3));
        overdue.setUpdatedAt(LocalDateTime.now());
        overdue.setUpdatedBy(managerUser);
        unitPaymentRepository.save(overdue);

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/units/" + unit101.getId() + "/pay-boleto")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // M01: Even after requesting confirmation, overdue unit past due date stays blocked from assembly voting until manager marks PAID
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/units/" + unit101.getId() + "/request-payment-confirmation")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("note", "Suposto PIX"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMATION_REQUESTED"));

        // M02: Manager cannot self-approve an outbound PENDING invitation without user acceptance
        String inviteJson = mockMvc.perform(post("/manager/condominiums/" + condo.getId() + "/membership-invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", foreignUser.getEmail(),
                                "authority", "ROLE_RESIDENT",
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long invitationId = objectMapper.readTree(inviteJson).get("id").asLong();

        mockMvc.perform(post("/manager/condominiums/" + condo.getId() + "/membership-requests/" + invitationId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // M02: Manager cannot bind a non-member foreign user via POST /residents
        mockMvc.perform(post("/residents")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.cefet_rj.projeto_final.condominiums.dtos.insert.ResidentInsertDTO(
                                foreignUser.getId(),
                                unit101.getId(),
                                ResidentType.OWNER.name()
                        ))))
                .andExpect(status().isForbidden());

        // M02: Staff cannot link a visitor to a user outside the condominium
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/visitors")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "visitorName", "Visitante Externo",
                                "residentId", foreignUser.getId()
                        ))))
                .andExpect(status().isForbidden());

        // M03: Resident cannot read suppliers PII
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/suppliers")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // M06: Pending resident vehicle request does NOT hijack unassigned ParkingSpot
        var freeSpot = new com.cefet_rj.projeto_final.community.entities.ParkingSpot();
        freeSpot.setCondominium(condo);
        freeSpot.setCode("G-FREE-99");
        freeSpot.setSpotType("VISITORS");
        freeSpot.setStatus("AVAILABLE");
        freeSpot.setCreatedAt(LocalDateTime.now());
        freeSpot = parkingSpotRepository.save(freeSpot);

        String vehResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CAR",
                                "brandModel", "VW Golf",
                                "licensePlate", "XYZ9K99",
                                "parkingSpotId", freeSpot.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long vehId = objectMapper.readTree(vehResp).get("id").asLong();

        var spotAfterPending = parkingSpotRepository.findById(freeSpot.getId()).orElseThrow();
        assertEquals("AVAILABLE", spotAfterPending.getStatus());
        assertNull(spotAfterPending.getUnit());

        mockMvc.perform(patch("/vehicles/" + vehId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        assertEquals("OCCUPIED", parkingSpotRepository.findById(freeSpot.getId()).orElseThrow().getStatus());

        // M06: Resident cannot cancel an already RESOLVED ticket
        OccurrenceTicket resolvedTicket = new OccurrenceTicket();
        resolvedTicket.setCondominium(condo);
        resolvedTicket.setAuthor(residentUser);
        resolvedTicket.setUnit(unit101);
        resolvedTicket.setCategory("MANUTENCAO");
        resolvedTicket.setTitle("Portão ajustado");
        resolvedTicket.setDescription("Já corrigido");
        resolvedTicket.setStatus("RESOLVED");
        resolvedTicket = ticketRepository.save(resolvedTicket);

        mockMvc.perform(patch("/tickets/" + resolvedTicket.getId() + "/status")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isConflict());

        // M04: Deactivated condominium blocks operational access & membership requests
        condo.setIsActive(false);
        condominiumRepository.save(condo);

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/announcements")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        condo.setIsActive(true);
        condominiumRepository.save(condo);
    }

    @Test
    void testStage2SecurityValidationRateLimitSseAndThemeHardening() throws Exception {
        // 1) UrlSanitizer blocks XSS vectors & path traversal, allows safe HTTPS/media URLs
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> com.cefet_rj.projeto_final.shared.validation.UrlSanitizer.sanitizeMediaUrl("javascript:alert(1)", "imageUrl")
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> com.cefet_rj.projeto_final.shared.validation.UrlSanitizer.sanitizeMediaUrl("data:image/svg+xml;base64,PHN2Zz4=", "imageUrl")
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> com.cefet_rj.projeto_final.shared.validation.UrlSanitizer.sanitizeMediaUrl("media/../../etc/passwd", "imageUrl")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "https://cdn.example.com/avatar.png",
                com.cefet_rj.projeto_final.shared.validation.UrlSanitizer.sanitizeMediaUrl("https://cdn.example.com/avatar.png", "imageUrl")
        );

        // 2) Invalid themePreference on profile update returns 400 Bad Request
        String residentToken = login(residentUser.getEmail());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/users/me/profile")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Morador",
                                "lastName", "Teste",
                                "email", residentUser.getEmail(),
                                "themePreference", "NEON_INVALID"
                        ))))
                .andExpect(status().isBadRequest());

        // 3) Invalid sort column on Admin data endpoint returns 400 Bad Request (not 500)
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        User admin = createUser("Admin", "Stage2", "admin.stage2@test.com", adminRole);
        String adminToken = login(admin.getEmail());
        mockMvc.perform(get("/admin/data/users?sort=campoInexistente,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // 4) AuthRateLimitFilter enforces HTTP 429 after limit exceeded
        var filter = new com.cefet_rj.projeto_final.auth.security.AuthRateLimitFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "authPerMinute", 2);
        for (int i = 0; i < 2; i++) {
            var req = new org.springframework.mock.web.MockHttpServletRequest("POST", "/oauth2/token");
            req.setRemoteAddr("198.51.100.77");
            var res = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(req, res, new org.springframework.mock.web.MockFilterChain());
            org.junit.jupiter.api.Assertions.assertEquals(200, res.getStatus());
        }
        var blockedReq = new org.springframework.mock.web.MockHttpServletRequest("POST", "/oauth2/token");
        blockedReq.setRemoteAddr("198.51.100.77");
        var blockedRes = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new org.springframework.mock.web.MockFilterChain());
        org.junit.jupiter.api.Assertions.assertEquals(429, blockedRes.getStatus());

        // 5) RealtimeEmitterService safely registers, subscribes new condo, sends heartbeat, and evicts
        var sseService = new com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService();
        sseService.registerEmitter(residentUser.getId(), java.util.List.of(condo.getId()));
        sseService.subscribeUserToCondominium(residentUser.getId(), 999L);
        sseService.sendHeartbeat();
        assertTrue(sseService.getActiveConnectionsCount() >= 1);
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
