package com.cefet_rj.projeto_final.community;

import com.cefet_rj.projeto_final.assemblies.entities.CondominiumAssembly;
import com.cefet_rj.projeto_final.assemblies.repositories.CondominiumAssemblyRepository;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.community.entities.MovingRequest;
import com.cefet_rj.projeto_final.community.entities.ParkingSpot;
import com.cefet_rj.projeto_final.community.entities.Vehicle;
import com.cefet_rj.projeto_final.community.entities.VisitorAccess;
import com.cefet_rj.projeto_final.community.repositories.MovingRequestRepository;
import com.cefet_rj.projeto_final.community.repositories.ParkingSpotRepository;
import com.cefet_rj.projeto_final.community.repositories.VehicleRepository;
import com.cefet_rj.projeto_final.community.repositories.VisitorAccessRepository;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.ResidentInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
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
import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
import com.cefet_rj.projeto_final.financial.repositories.CondominiumExpenseRepository;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
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
class BusinessRulesIntegrationTests {

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
    @Autowired private CondominiumAssemblyRepository assemblyRepository;
    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CondominiumExpenseRepository expenseRepository;
    @Autowired private ParkingSpotRepository parkingSpotRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private MovingRequestRepository movingRequestRepository;
    @Autowired private VisitorAccessRepository visitorAccessRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condo;
    private Sector blockA;
    private Unit unit101;
    private User managerUser;
    private User residentUser;
    private CommonArea salon;

    @BeforeEach
    void setUp() {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES);
        Feature announcementsFeature = getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        BillingPlan plan = new BillingPlan();
        plan.setName("TEST_BUSINESS_RULES_PLAN");
        plan.setMonthlyFee(new BigDecimal("250.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, packagesFeature, announcementsFeature));
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Residencial Regras de Negocio");
        condo.setAddress("Avenida Central, 500");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        blockA = new Sector();
        blockA.setName("Bloco Alpha");
        blockA.setStructuralType(SectorStructuralType.BLOCK);
        blockA.setCondominium(condo);
        blockA = sectorRepository.save(blockA);

        unit101 = new Unit();
        unit101.setIdentifier("101");
        unit101.setSector(blockA);
        unit101 = unitRepository.save(unit101);

        salon = new CommonArea();
        salon.setName("Salão de Festas Principal");
        salon.setCapacity(50);
        salon.setCondominium(condo);
        salon.setActive(true);
        salon.setRequiresApproval(true);
        salon = commonAreaRepository.save(salon);

        managerUser = createUser("Carlos", "Sindico", "sindico.regras@condo.com", managerRole);
        residentUser = createUser("Mariana", "Moradora", "moradora.regras@condo.com", residentRole);

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

    // =========================================================================
    // 1. ASSEMBLEIAS: REGRAS DE CICLO DE VIDA E VALIDAÇÃO DE DATAS
    // =========================================================================

    @Test
    void testAssemblyCannotBeCreatedWithPastMeetingDate() throws Exception {
        String token = login(managerUser.getEmail());

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/assemblies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Assembleia Geral Ordinária",
                                "meetingDate", LocalDateTime.now().minusDays(1).toString(),
                                "meetingType", "ORDINARY"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAssemblyCannotBeReopenedAfterConcluded() throws Exception {
        String token = login(managerUser.getEmail());

        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setCondominium(condo);
        assembly.setTitle("Assembleia Extraordinária de Obras");
        assembly.setMeetingDate(LocalDateTime.now().plusDays(5));
        assembly.setStatus("SCHEDULED");
        assembly = assemblyRepository.save(assembly);

        // Advance to CONCLUDED
        mockMvc.perform(patch("/assemblies/" + assembly.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CONCLUDED"
                        ))))
                .andExpect(status().isOk());

        // Attempt to reopen to IN_PROGRESS -> Must return 409 Conflict
        mockMvc.perform(patch("/assemblies/" + assembly.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "IN_PROGRESS"
                        ))))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 2. RESERVAS: PREVENÇÃO DE DOUBLE BOOKING E CONSISTÊNCIA DE STATUS
    // =========================================================================

    @Test
    void testReservationCreationRejectsInvalidDates() throws Exception {
        String token = login(residentUser.getEmail());

        // endAt before startAt
        mockMvc.perform(post("/common-areas/" + salon.getId() + "/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "commonAreaId", salon.getId(),
                                "startAt", LocalDateTime.now().plusDays(3).toString(),
                                "endAt", LocalDateTime.now().plusDays(2).toString(),
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isBadRequest());

        // startAt in past
        mockMvc.perform(post("/common-areas/" + salon.getId() + "/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "commonAreaId", salon.getId(),
                                "startAt", LocalDateTime.now().minusDays(1).toString(),
                                "endAt", LocalDateTime.now().plusHours(2).toString(),
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReservationApprovalRejectsOverlappingConfirmedBooking() throws Exception {
        String mgrToken = login(managerUser.getEmail());

        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        // Pre-existing CONFIRMED reservation
        Reservation confirmedRes = new Reservation();
        confirmedRes.setCommonArea(salon);
        confirmedRes.setReservedBy(residentUser);
        confirmedRes.setStartAt(start);
        confirmedRes.setEndAt(end);
        confirmedRes.setStatus(ReservationStatus.CONFIRMED);
        confirmedRes.setCreatedAt(LocalDateTime.now());
        reservationRepository.save(confirmedRes);

        // Second reservation in PENDING status overlapping the same slot
        Reservation conflictingRes = new Reservation();
        conflictingRes.setCommonArea(salon);
        conflictingRes.setReservedBy(managerUser);
        conflictingRes.setStartAt(start.plusHours(1));
        conflictingRes.setEndAt(end.plusHours(2));
        conflictingRes.setStatus(ReservationStatus.PENDING);
        conflictingRes.setCreatedAt(LocalDateTime.now());
        conflictingRes = reservationRepository.save(conflictingRes);

        // Manager tries to approve conflicting reservation -> 409 Conflict
        mockMvc.perform(patch("/reservations/" + conflictingRes.getId() + "/approve")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 3. FINANCEIRO: INTEGRIDADE DE DESPESAS E PAGAMENTOS
    // =========================================================================

    @Test
    void testExpenseCreationRejectsNegativeAmountAndPreventsIllegalStatusChanges() throws Exception {
        String token = login(managerUser.getEmail());

        // Negative expense amount
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Compra de Materiais",
                                "description", "Compra de Materiais",
                                "amount", "-150.00",
                                "dueDate", LocalDate.now().plusDays(15).toString()
                        ))))
                .andExpect(status().isBadRequest());

        // Create valid expense
        CondominiumExpense expense = new CondominiumExpense();
        expense.setCondominium(condo);
        expense.setCategory("MANUTENCAO");
        expense.setDescription("Serviço de Pintura");
        expense.setAmount(new BigDecimal("500.00"));
        expense.setDueDate(LocalDate.now().plusDays(10));
        expense.setStatus("PAID");
        expense.setPaidAt(LocalDateTime.now());
        expense = expenseRepository.save(expense);

        // Attempt to cancel an already PAID expense -> 409 Conflict
        mockMvc.perform(patch("/financial/expenses/" + expense.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 4. MUDANÇAS: VALIDAÇÃO DE DATAS E CONFLITOS DE HORÁRIO DE ELEVADOR
    // =========================================================================

    @Test
    void testMovingRequestCannotBeInPastAndPreventsConflictingApprovals() throws Exception {
        String residentToken = login(residentUser.getEmail());
        String mgrToken = login(managerUser.getEmail());

        // Past moving date -> 400 Bad Request
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/moving-requests")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "movingDate", LocalDate.now().minusDays(2).toString(),
                                "period", "MORNING",
                                "type", "MOVE_IN"
                        ))))
                .andExpect(status().isBadRequest());

        LocalDate futureDate = LocalDate.now().plusDays(7);

        // Pre-existing APPROVED move in the MORNING
        MovingRequest req1 = new MovingRequest();
        req1.setCondominium(condo);
        req1.setUnit(unit101);
        req1.setRequestedBy(residentUser);
        req1.setType("MOVE_IN");
        req1.setMovingDate(futureDate);
        req1.setPeriod("MORNING");
        req1.setStatus("APPROVED");
        movingRequestRepository.save(req1);

        // Second move requested for the same MORNING
        MovingRequest req2 = new MovingRequest();
        req2.setCondominium(condo);
        req2.setUnit(unit101);
        req2.setRequestedBy(residentUser);
        req2.setType("MOVE_OUT");
        req2.setMovingDate(futureDate);
        req2.setPeriod("MORNING");
        req2.setStatus("PENDING");
        req2 = movingRequestRepository.save(req2);

        // Manager tries to approve req2 -> 409 Conflict
        mockMvc.perform(patch("/moving-requests/" + req2.getId() + "/approve")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 5. VEÍCULOS E VAGAS DE GARAGEM: EXCLUSIVIDADE E LIBERAÇÃO EM RECUSA
    // =========================================================================

    @Test
    void testParkingSpotExclusivityAndReleaseOnRejection() throws Exception {
        String mgrToken = login(managerUser.getEmail());

        ParkingSpot spot = new ParkingSpot();
        spot.setCondominium(condo);
        spot.setCode("VAGA-101");
        spot.setStatus("AVAILABLE");
        spot = parkingSpotRepository.save(spot);

        // Link vehicle 1 to spot -> spot becomes OCCUPIED
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "brandModel", "Toyota Corolla",
                                "licensePlate", "ABC1D23",
                                "parkingSpotId", spot.getId()
                        ))))
                .andExpect(status().isCreated());

        ParkingSpot updatedSpot = parkingSpotRepository.findById(spot.getId()).orElseThrow();
        assertEquals("OCCUPIED", updatedSpot.getStatus());

        // Second vehicle tries to take the same spot -> 409 Conflict
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "brandModel", "Honda Civic",
                                "licensePlate", "XYZ9E87",
                                "parkingSpotId", spot.getId()
                        ))))
                .andExpect(status().isConflict());

        // Reject vehicle 1 -> spot must be restored to AVAILABLE
        Vehicle vehicle1 = vehicleRepository.findByCondominiumIdAndLicensePlateContainingIgnoreCaseOrderByCreatedAtDesc(condo.getId(), "ABC1D23").get(0);
        mockMvc.perform(patch("/vehicles/" + vehicle1.getId() + "/reject")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isOk());

        ParkingSpot freedSpot = parkingSpotRepository.findById(spot.getId()).orElseThrow();
        assertEquals("AVAILABLE", freedSpot.getStatus());
    }

    // =========================================================================
    // 6. VISITANTES: CICLO DE VIDA DE CHECK-IN E CHECK-OUT
    // =========================================================================

    @Test
    void testVisitorAccessCannotCheckOutWithoutCheckIn() throws Exception {
        String mgrToken = login(managerUser.getEmail());

        VisitorAccess access = new VisitorAccess();
        access.setCondominium(condo);
        access.setResident(residentUser);
        access.setUnit(unit101);
        access.setVisitorName("Carlos Convidado");
        access.setAccessCode("VA-998877");
        access.setStatus("AUTHORIZED");
        access = visitorAccessRepository.save(access);

        // Try to CHECK_OUT without checking in -> 409 Conflict
        mockMvc.perform(patch("/visitors/" + access.getId() + "/check-out")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isConflict());

        // Perform CHECK_IN -> Success
        mockMvc.perform(patch("/visitors/" + access.getId() + "/check-in")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isOk());

        // Perform CHECK_OUT -> Success
        mockMvc.perform(patch("/visitors/" + access.getId() + "/check-out")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isOk());

        // Attempting to CHECK_IN again after checkout -> 409 Conflict
        mockMvc.perform(patch("/visitors/" + access.getId() + "/check-in")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 7. ESTRUTURA: PREVENÇÃO DE DUPLICATAS EM SETORES, UNIDADES E MORADORES
    // =========================================================================

    @Test
    void testDuplicatePreventionForSectorsUnitsAndResidents() throws Exception {
        String mgrToken = login(managerUser.getEmail());

        // Duplicate sector name in the same condominium -> 409 Conflict
        mockMvc.perform(post("/sectors")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SectorInsertDTO(
                                "Bloco Alpha",
                                SectorStructuralType.BLOCK.name(),
                                condo.getId(),
                                null
                        ))))
                .andExpect(status().isConflict());

        // Duplicate unit identifier in the same sector -> 409 Conflict
        mockMvc.perform(post("/units")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UnitInsertDTO(
                                "101",
                                blockA.getId()
                        ))))
                .andExpect(status().isConflict());

        // Duplicate resident for the same user and unit -> 409 Conflict
        mockMvc.perform(post("/residents")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResidentInsertDTO(
                                residentUser.getId(),
                                unit101.getId(),
                                ResidentType.TENANT.name()
                        ))))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Role getOrCreateRole(String roleName) {
        Role role = roleRepository.findByAuthority(roleName);
        if (role == null) {
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
