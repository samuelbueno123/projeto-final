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
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
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
public class SecurityAndCommercialFeaturesIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

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
    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private UnitPaymentRepository unitPaymentRepository;

    private Condominium condoA;
    private Condominium condoB;
    private Sector sectorA;
    private Unit unit101;
    private Unit unit102;

    private User managerA;
    private User managerB;
    private User doormanA;
    private User resident1;
    private User resident2;

    private String managerAToken;
    private String managerBToken;
    private String doormanAToken;
    private String resident1Token;
    private String resident2Token;

    @BeforeEach
    void setUp() throws Exception {
        Role roleManager = ensureRole("ROLE_MANAGER");
        Role roleDoorman = ensureRole("ROLE_DOORMAN");
        Role roleResident = ensureRole("ROLE_RESIDENT");

        BillingPlan fullPlan = ensureFullBillingPlan();

        condoA = condominiumRepository.findAll().stream()
                .filter(c -> "Condomínio Auditoria A".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Condominium c = new Condominium();
                    c.setName("Condomínio Auditoria A");
                    c.setAddress("Rua A, 100");
                    c.setIsActive(true);
                    c.setPlanSelected(true);
                    c.setBillingPlan(fullPlan);
                    return condominiumRepository.save(c);
                });

        condoB = condominiumRepository.findAll().stream()
                .filter(c -> "Condomínio Auditoria B".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Condominium c = new Condominium();
                    c.setName("Condomínio Auditoria B");
                    c.setAddress("Rua B, 200");
                    c.setIsActive(true);
                    c.setPlanSelected(true);
                    c.setBillingPlan(fullPlan);
                    return condominiumRepository.save(c);
                });

        sectorA = sectorRepository.findByCondominiumId(condoA.getId()).stream()
                .findFirst()
                .orElseGet(() -> {
                    Sector s = new Sector();
                    s.setName("Bloco Único");
                    s.setStructuralType(SectorStructuralType.BLOCK);
                    s.setCondominium(condoA);
                    return sectorRepository.save(s);
                });

        unit101 = ensureUnit(sectorA, "101");
        unit102 = ensureUnit(sectorA, "102");

        managerA = ensureUser("manager_a_audit@test.com", "Síndico", "CondoA", roleManager);
        managerB = ensureUser("manager_b_audit@test.com", "Síndico", "CondoB", roleManager);
        doormanA = ensureUser("doorman_a_audit@test.com", "Porteiro", "CondoA", roleDoorman);
        resident1 = ensureUser("resident_1_audit@test.com", "Morador", "Um", roleResident);
        resident2 = ensureUser("resident_2_audit@test.com", "Morador", "Dois", roleResident);

        ensureMembership(managerA, condoA, roleManager, null);
        ensureMembership(managerB, condoB, roleManager, null);
        ensureMembership(doormanA, condoA, roleDoorman, null);
        ensureMembership(resident1, condoA, roleResident, unit101);
        ensureMembership(resident2, condoA, roleResident, unit102);

        ensureResident(resident1, unit101);
        ensureResident(resident2, unit102);

        managerAToken = login(managerA.getEmail());
        managerBToken = login(managerB.getEmail());
        doormanAToken = login(doormanA.getEmail());
        resident1Token = login(resident1.getEmail());
        resident2Token = login(resident2.getEmail());
    }

    @Test
    void testSecurityAccessControlAndIdorProtection() throws Exception {
        // 1. Resident cannot list all users or view another user's profile via /users
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users/" + resident2.getId())
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isForbidden());

        // 2. Resident 1 cannot access Resident 2's unit payment status (IDOR / BOLA)
        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/units/" + unit102.getId() + "/payment-status")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/units/" + unit102.getId() + "/pay-boleto")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isForbidden());

        // 3. Doorman cannot execute Manager-only operations
        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/parking-spots")
                        .header("Authorization", "Bearer " + doormanAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "G-99"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/suppliers")
                        .header("Authorization", "Bearer " + doormanAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Fornecedor X", "category", "ELEVATOR"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reports/dashboard")
                        .header("Authorization", "Bearer " + doormanAToken))
                .andExpect(status().isForbidden());

        // 4. Cross-tenant isolation: Manager of Condo B cannot access or mutate Condo A
        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reports/dashboard")
                        .header("Authorization", "Bearer " + managerBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/parking-spots")
                        .header("Authorization", "Bearer " + managerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "G-INVASION"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testVehiclesParkingSpotsAndPetsWorkflow() throws Exception {
        // Manager creates ParkingSpot G-01 associated with unit 101
        String spotResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/parking-spots")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "G-01",
                                "description", "Subsolo 1",
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("G-01"))
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andReturn().getResponse().getContentAsString();
        long spotId = objectMapper.readTree(spotResp).get("id").asLong();

        // Resident 1 registers a vehicle linked to spot G-01
        String vehResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CAR",
                                "brandModel", "Toyota Corolla",
                                "color", "Prata",
                                "licensePlate", "ABC1D23",
                                "parkingSpotId", spotId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.parkingSpot").value("G-01"))
                .andReturn().getResponse().getContentAsString();
        long vehicleId = objectMapper.readTree(vehResp).get("id").asLong();

        // Duplicate license plate in the same condominium is rejected with 409 Conflict
        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + resident2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CAR",
                                "brandModel", "Honda Civic",
                                "color", "Preto",
                                "licensePlate", "ABC-1D23"
                        ))))
                .andExpect(status().isConflict());

        // Manager approves the vehicle
        mockMvc.perform(patch("/vehicles/" + vehicleId + "/approve")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Resident 1 registers a Pet and updates its vaccination info
        String petResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/pets")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Thor",
                                "species", "DOG",
                                "breed", "Golden Retriever",
                                "size", "LARGE",
                                "color", "Dourado",
                                "vaccinated", true,
                                "lastVaccineDate", LocalDate.now().minusMonths(2).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Thor"))
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(petResp).get("id").asLong();

        mockMvc.perform(put("/pets/" + petId)
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Thor Atualizado",
                                "notes", "Dócil e vacinado"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thor Atualizado"))
                .andExpect(jsonPath("$.notes").value("Dócil e vacinado"));
    }

    @Test
    void testPreventiveMaintenanceAndSuppliersWorkflow() throws Exception {
        // Manager registers a Supplier
        String supResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/suppliers")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Atlas Elevadores Ltda",
                                "serviceCategory", "ELEVATOR",
                                "contactName", "Carlos",
                                "phone", "21999998888",
                                "email", "contato@atlas.com"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Atlas Elevadores Ltda"))
                .andReturn().getResponse().getContentAsString();
        long supplierId = objectMapper.readTree(supResp).get("id").asLong();

        // Manager registers an overdue MaintenanceEquipment with 30-day recurrence
        String eqResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/maintenance/equipment")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Elevador Social 01",
                                "category", "ELEVATOR",
                                "location", "Bloco Único",
                                "recurrenceDays", 30,
                                "nextMaintenanceDate", LocalDate.now().minusDays(3).toString(),
                                "preferredSupplierId", supplierId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(true))
                .andReturn().getResponse().getContentAsString();
        long equipmentId = objectMapper.readTree(eqResp).get("id").asLong();

        // Manager creates a Preventive MaintenanceOrder linked to equipment and supplier
        String orderResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/maintenance/orders")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Manutenção Preventiva Mensal - Elevador 01",
                                "description", "Lubrificação e inspeção de cabos",
                                "category", "ELEVATOR",
                                "type", "PREVENTIVE",
                                "priority", "HIGH",
                                "equipmentId", equipmentId,
                                "supplierId", supplierId,
                                "estimatedCost", 850.00
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(orderResp).get("id").asLong();

        // Completing the order automatically advances equipment's nextMaintenanceDate and evaluates supplier
        mockMvc.perform(patch("/maintenance/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED",
                                "actualCost", 850.00,
                                "supplierRating", 5,
                                "supplierFeedback", "Excelente atendimento técnico",
                                "notes", "Serviço executado dentro do prazo"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.supplierRating").value(5));

        // Verify equipment is no longer overdue
        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/maintenance/equipment")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overdue").value(false));

        // Verify supplier average rating updated to 5.0
        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/suppliers")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ratingCount").value(1))
                .andExpect(jsonPath("$[0].ratingAverage").value(5.0));
    }

    @Test
    void testDigitalAssemblyAttendanceQuorumAndVotingRules() throws Exception {
        // Mark unit 102 as OVERDUE to test delinquency eligibility block
        UnitPayment up102 = unitPaymentRepository.findByUnitId(unit102.getId()).orElseGet(UnitPayment::new);
        up102.setUnit(unit102);
        up102.setStatus(UnitPaymentStatus.OVERDUE);
        up102.setAmount(new BigDecimal("650.00"));
        up102.setDueDate(LocalDate.now().minusDays(10));
        up102.setUpdatedAt(LocalDateTime.now());
        up102.setUpdatedBy(managerA);
        unitPaymentRepository.save(up102);

        // Manager creates an assembly with blockDelinquentUnits = true
        String asmResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/assemblies")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Assembleia Geral Ordinária 2026",
                                "description", "Deliberação sobre obras e contas",
                                "meetingDate", LocalDateTime.now().plusDays(2).toString(),
                                "locationOrLink", "Plenário Virtual",
                                "blockDelinquentUnits", true,
                                "quorumMinimumPercent", 50
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode asmJson = objectMapper.readTree(asmResp);
        long assemblyId = asmJson.get("id").asLong();

        // Add topic to assembly
        String topicResp = mockMvc.perform(post("/assemblies/" + assemblyId + "/topics")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Aprovação da reforma da fachada",
                                "description", "Orçamento de R$ 50.000",
                                "displayOrder", 1
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long topicId = objectMapper.readTree(topicResp).get("id").asLong();

        // Open topic for voting
        mockMvc.perform(patch("/assembly-topics/" + topicId + "/voting")
                        .param("open", "true")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votingOpen").value(true));

        // Resident 1 checks in to the assembly
        mockMvc.perform(post("/assemblies/" + assemblyId + "/check-in")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("proxyInfo", "Titular da Unidade 101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitIdentifier").value("101"));

        // Resident 1 casts vote for unit 101
        mockMvc.perform(post("/assembly-topics/" + topicId + "/votes")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("voteChoice", "IN_FAVOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userVote").value("IN_FAVOR"))
                .andExpect(jsonPath("$.inFavorCount").value(1));

        // Resident 2 (Unit 102 OVERDUE) is blocked from voting because blockDelinquentUnits=true
        mockMvc.perform(post("/assembly-topics/" + topicId + "/votes")
                        .header("Authorization", "Bearer " + resident2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("voteChoice", "IN_FAVOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAnnouncementReadReceiptsAndReservationWaitlistPromotion() throws Exception {
        // 1. Manager creates segmented announcement
        String annResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/announcements")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Manutenção da Caixa D'Água",
                                "content", "Abastecimento interrompido das 08h às 12h.",
                                "sectorId", sectorA.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sectorId").value(sectorA.getId()))
                .andReturn().getResponse().getContentAsString();
        long annId = objectMapper.readTree(annResp).get("id").asLong();

        // Resident 1 marks announcement as read
        mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readByCurrentUser").value(true))
                .andExpect(jsonPath("$.readCount").value(1));

        // Manager lists announcement readers
        mockMvc.perform(get("/announcements/" + annId + "/reads")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(resident1.getId()));

        // 2. Reservation Waitlist & Automatic Promotion
        CommonArea grill = new CommonArea();
        grill.setName("Churrasqueira Gourmet");
        grill.setCapacity(30);
        grill.setActive(true);
        grill.setRequiresApproval(true);
        grill.setAllowWaitlist(true);
        grill.setCondominium(condoA);
        grill = commonAreaRepository.save(grill);

        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(4);

        // Resident 1 books the slot -> PENDING
        String res1Resp = mockMvc.perform(post("/common-areas/" + grill.getId() + "/reservations")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startAt", start.toString(),
                                "endAt", end.toString(),
                                "notes", "Aniversário"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long res1Id = objectMapper.readTree(res1Resp).get("id").asLong();

        // Resident 2 joins the waitlist for the same slot -> WAITLISTED (position 1)
        String res2Resp = mockMvc.perform(post("/common-areas/" + grill.getId() + "/waitlist")
                        .header("Authorization", "Bearer " + resident2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startAt", start.toString(),
                                "endAt", end.toString(),
                                "notes", "Fila de espera"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1))
                .andReturn().getResponse().getContentAsString();
        long res2Id = objectMapper.readTree(res2Resp).get("id").asLong();

        // Resident 1 cancels their reservation -> Resident 2 is automatically promoted to PENDING!
        mockMvc.perform(patch("/reservations/" + res1Id + "/cancel")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reservations/my")
                        .header("Authorization", "Bearer " + resident2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + res2Id + ")].status").value("PENDING"));
    }

    @Test
    void testAdvancedVisitorQrValidationLostFoundAndReportsExport() throws Exception {
        // 1. Resident 1 pre-authorizes visitor with configurable validity
        String visResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/visitors")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "visitorName", "João Entregador",
                                "documentNumber", "12345678900",
                                "type", "VISITOR",
                                "validFrom", LocalDateTime.now().minusHours(1).toString(),
                                "validUntil", LocalDateTime.now().plusHours(6).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCodePayload").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode visJson = objectMapper.readTree(visResp);
        long visitorId = visJson.get("id").asLong();
        String qrCodePayload = visJson.get("qrCodePayload").asText();

        // Resident cannot check in the visitor (staff only -> 403)
        mockMvc.perform(patch("/visitors/" + visitorId + "/check-in")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isForbidden());

        // Doorman validates QR code and checks in visitor
        mockMvc.perform(post("/condominiums/" + condoA.getId() + "/visitors/validate-qr")
                        .header("Authorization", "Bearer " + doormanAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "qrCodeOrAccessCode", qrCodePayload,
                                "checkIn", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInByName").isNotEmpty());

        // 2. Lost & Found with category, communication messages, and closing
        String lfResp = mockMvc.perform(post("/condominiums/" + condoA.getId() + "/lost-found")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "FOUND",
                                "category", "KEYS",
                                "title", "Chaveiro com tag azul",
                                "description", "Encontrado próximo à portaria",
                                "locationFoundOrLost", "Hall Social"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("KEYS"))
                .andReturn().getResponse().getContentAsString();
        long lfId = objectMapper.readTree(lfResp).get("id").asLong();

        // Resident 2 sends a message on the Lost & Found item
        mockMvc.perform(post("/lost-found/" + lfId + "/messages")
                        .header("Authorization", "Bearer " + resident2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "Olá! Perdi uma chave assim hoje cedo, posso retirar?"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderName").isNotEmpty());

        // Resident 2 (not reporter or manager) cannot close Resident 1's publication
        mockMvc.perform(patch("/lost-found/" + lfId + "/close")
                        .header("Authorization", "Bearer " + resident2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resolutionNotes", "Tentativa indevida"))))
                .andExpect(status().isForbidden());

        // Resident 1 (reporter) closes the publication
        mockMvc.perform(patch("/lost-found/" + lfId + "/close")
                        .header("Authorization", "Bearer " + resident1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resolutionNotes", "Entregue ao vizinho da 102"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 3. Managerial Reports & CSV/PDF Export
        mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reports/dashboard")
                        .param("startDate", LocalDate.now().minusDays(7).toString())
                        .param("endDate", LocalDate.now().plusDays(7).toString())
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitors.total").isNumber())
                .andExpect(jsonPath("$.maintenance.totalOrders").isNumber())
                .andExpect(jsonPath("$.financial.totalInvoiced").isNumber());

        byte[] csvBytes = mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reports/export/csv")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(csvBytes, StandardCharsets.UTF_8).contains("Relatorio Gerencial"));

        byte[] pdfBytes = mockMvc.perform(get("/condominiums/" + condoA.getId() + "/reports/export/pdf")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdfBytes, StandardCharsets.US_ASCII).startsWith("%PDF-1.4"));
    }

    private Role ensureRole(String authority) {
        Role found = roleRepository.findByAuthority(authority);
        if (found != null) return found;
        Role role = new Role();
        role.setAuthority(authority);
        return roleRepository.save(role);
    }

    private BillingPlan ensureFullBillingPlan() {
        BillingPlan plan = billingPlanRepository.findAll().stream()
                .filter(p -> "Plano Auditoria Completo".equals(p.getName()))
                .findFirst()
                .orElseGet(() -> {
                    BillingPlan bp = new BillingPlan();
                    bp.setName("Plano Auditoria Completo");
                    bp.setMonthlyFee(new BigDecimal("499.00"));
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

    private Unit ensureUnit(Sector sector, String identifier) {
        return unitRepository.findAllForCondominium(sector.getCondominium().getId()).stream()
                .filter(u -> identifier.equals(u.getIdentifier()))
                .findFirst()
                .orElseGet(() -> {
                    Unit u = new Unit();
                    u.setIdentifier(identifier);
                    u.setSector(sector);
                    return unitRepository.save(u);
                });
    }

    private User ensureUser(String email, String firstName, String lastName, Role role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            u.addRole(role);
            return userRepository.save(u);
        });
    }

    private void ensureMembership(User user, Condominium condo, Role role, Unit unit) {
        boolean exists = membershipRepository.findAllByUserId(user.getId()).stream()
                .anyMatch(m -> m.getCondominium().getId().equals(condo.getId())
                        && m.getRole().getAuthority().equals(role.getAuthority()));
        if (!exists) {
            Membership m = new Membership();
            m.setUser(user);
            m.setCondominium(condo);
            m.setRole(role);
            m.setStatus(MembershipStatus.ACTIVE);
            m.setRequestedUnit(unit);
            m.setCreatedAt(LocalDateTime.now());
            membershipRepository.save(m);
        }
    }

    private void ensureResident(User user, Unit unit) {
        boolean exists = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(
                user.getId(), unit.getSector().getCondominium().getId()
        ).isPresent();
        if (!exists) {
            Resident r = new Resident();
            r.setUser(user);
            r.setUnit(unit);
            r.setType(ResidentType.OWNER);
            residentRepository.save(r);
        }
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
