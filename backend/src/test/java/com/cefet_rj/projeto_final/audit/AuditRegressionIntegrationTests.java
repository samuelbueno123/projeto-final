package com.cefet_rj.projeto_final.audit;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import com.cefet_rj.projeto_final.community.entities.VisitorAccess;
import com.cefet_rj.projeto_final.community.repositories.DoormanChatMessageRepository;
import com.cefet_rj.projeto_final.community.repositories.VisitorAccessRepository;
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
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.history.repositories.PackageHistoryRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.exceptions.ReservationConflictException;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.reservations.services.ReservationService;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
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
class AuditRegressionIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ResidentRepository residentRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private MembershipRepository membershipRepository;

    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationService reservationService;

    @Autowired private PackageRepository packageRepository;
    @Autowired private PackageHistoryRepository packageHistoryRepository;
    @Autowired private VisitorAccessRepository visitorAccessRepository;
    @Autowired private DoormanChatMessageRepository chatMessageRepository;

    private Condominium condo1;
    private Condominium condo2;
    private Unit unit101;
    private Unit unit102;
    private Unit unit201;
    private CommonArea salon;
    private User managerUser;
    private User residentUser;
    private User otherResident;

    @BeforeEach
    void setUp() {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES);
        Feature notificationsFeature = getOrCreateFeature(FeatureCode.NOTIFICATIONS);
        Feature announcementsFeature = getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        BillingPlan plan = new BillingPlan();
        plan.setName("AUDIT_REGRESSION_PLAN");
        plan.setMonthlyFee(new BigDecimal("299.90"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, packagesFeature, notificationsFeature, announcementsFeature));
        plan = billingPlanRepository.save(plan);

        condo1 = new Condominium();
        condo1.setName("Condomínio Auditoria 1");
        condo1.setAddress("Rua A, 100");
        condo1.setBillingPlan(plan);
        condo1.setIsActive(true);
        condo1 = condominiumRepository.save(condo1);

        Sector sector1 = new Sector();
        sector1.setName("Bloco A");
        sector1.setStructuralType(SectorStructuralType.BLOCK);
        sector1.setCondominium(condo1);
        sector1 = sectorRepository.save(sector1);

        unit101 = new Unit();
        unit101.setIdentifier("101");
        unit101.setSector(sector1);
        unit101 = unitRepository.save(unit101);

        unit102 = new Unit();
        unit102.setIdentifier("102");
        unit102.setSector(sector1);
        unit102 = unitRepository.save(unit102);

        condo2 = new Condominium();
        condo2.setName("Condomínio Auditoria 2");
        condo2.setAddress("Rua B, 200");
        condo2.setBillingPlan(plan);
        condo2.setIsActive(true);
        condo2 = condominiumRepository.save(condo2);

        Sector sector2 = new Sector();
        sector2.setName("Bloco B");
        sector2.setStructuralType(SectorStructuralType.BLOCK);
        sector2.setCondominium(condo2);
        sector2 = sectorRepository.save(sector2);

        unit201 = new Unit();
        unit201.setIdentifier("201");
        unit201.setSector(sector2);
        unit201 = unitRepository.save(unit201);

        salon = new CommonArea();
        salon.setName("Salão Audit");
        salon.setCapacity(40);
        salon.setCondominium(condo1);
        salon.setActive(true);
        salon.setRequiresApproval(true);
        salon.setMinAdvanceHours(1);
        salon = commonAreaRepository.save(salon);

        managerUser = createUser("Sindico", "Audit", "sindico.audit@test.com", managerRole);
        residentUser = createUser("Morador", "Principal", "morador1.audit@test.com", residentRole);
        otherResident = createUser("Outro", "Morador", "morador2.audit@test.com", residentRole);

        saveActiveMembership(managerUser, condo1, managerRole, null);
        saveActiveMembership(residentUser, condo1, residentRole, unit101);
        saveActiveMembership(otherResident, condo1, residentRole, unit102);

        saveResidentLink(residentUser, unit101);
        saveResidentLink(otherResident, unit102);
    }

    @Test
    void testReservationCounterProposeRejectsCancelledAndModifyAndConfirmDetectsConflict() throws Exception {
        String managerToken = login(managerUser.getEmail());

        LocalDateTime baseStart = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);

        Reservation confirmed = new Reservation();
        confirmed.setCommonArea(salon);
        confirmed.setReservedBy(otherResident);
        confirmed.setStartAt(baseStart);
        confirmed.setEndAt(baseStart.plusHours(2));
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        confirmed.setCreatedAt(LocalDateTime.now());
        reservationRepository.save(confirmed);

        Reservation pending = new Reservation();
        pending.setCommonArea(salon);
        pending.setReservedBy(residentUser);
        pending.setStartAt(baseStart.minusHours(4));
        pending.setEndAt(baseStart.minusHours(2));
        pending.setStatus(ReservationStatus.PENDING);
        pending.setCreatedAt(LocalDateTime.now());
        pending = reservationRepository.save(pending);

        Reservation cancelled = new Reservation();
        cancelled.setCommonArea(salon);
        cancelled.setReservedBy(residentUser);
        cancelled.setStartAt(baseStart.plusDays(1));
        cancelled.setEndAt(baseStart.plusDays(1).plusHours(2));
        cancelled.setStatus(ReservationStatus.CANCELLED);
        cancelled.setCreatedAt(LocalDateTime.now());
        cancelled.setCancelledAt(LocalDateTime.now());
        cancelled = reservationRepository.save(cancelled);

        ReservationDTO.CounterProposalDTO proposalDto = new ReservationDTO.CounterProposalDTO(
                baseStart.plusDays(2),
                baseStart.plusDays(2).plusHours(2),
                "Novo horário sugerido"
        );
        mockMvc.perform(patch("/reservations/" + cancelled.getId() + "/counter-propose")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proposalDto)))
                .andExpect(status().is4xxClientError());

        Long pendingId = pending.getId();
        ReservationDTO.ModifyAndConfirmDTO conflictingModify = new ReservationDTO.ModifyAndConfirmDTO(
                baseStart.plusMinutes(30),
                baseStart.plusHours(1).plusMinutes(30),
                "Tentando mover para horário ocupado"
        );

        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(managerUser.getEmail(), "N/A")
            );
            assertThrows(
                    ReservationConflictException.class,
                    () -> reservationService.modifyAndConfirm(pendingId, conflictingModify)
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testPackageDeleteCleansHistoryAndExceptionHandlersMapStatuses() throws Exception {
        String managerToken = login(managerUser.getEmail());

        String createdJson = mockMvc.perform(post("/packages")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PackageInsertDTO(
                                condo1.getId(),
                                "Caixa Amazon Auditoria",
                                residentUser.getId(),
                                unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long packageId = objectMapper.readTree(createdJson).get("id").asLong();
        assertFalse(packageHistoryRepository.findByPackageEntityIdOrderByPerformedAtAsc(packageId).isEmpty());

        mockMvc.perform(delete("/packages/" + packageId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        assertTrue(packageRepository.findById(packageId).isEmpty());
        assertTrue(packageHistoryRepository.findByPackageEntityIdOrderByPerformedAtAsc(packageId).isEmpty());

        // InvalidStatusException -> 400 Bad Request
        mockMvc.perform(get("/packages")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("condominiumId", condo1.getId().toString())
                        .param("status", "STATUS_INEXISTENTE"))
                .andExpect(status().isBadRequest());

        // InvalidPackageStateTransitionException -> 409 Conflict (EXPECTED -> CANCELLED -> NOTIFIED inválido)
        String residentToken = login(residentUser.getEmail());
        String expectedJson = mockMvc.perform(post("/packages/expected")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PackageInsertDTO(
                                condo1.getId(),
                                "Entrega Esperada",
                                residentUser.getId(),
                                unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long expectedId = objectMapper.readTree(expectedJson).get("id").asLong();
        mockMvc.perform(patch("/packages/" + expectedId + "/cancel")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/packages/" + expectedId + "/notify")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void testCommunityDuplicateKeyCrossCondoUtilityReadingAndDigitalPassOwnership() throws Exception {
        String managerToken = login(managerUser.getEmail());
        String residentToken = login(residentUser.getEmail());
        String otherResidentToken = login(otherResident.getEmail());

        // Chave duplicada no mesmo condomínio -> 409 Conflict
        Map<String, Object> keyBody = Map.of(
                "keyCode", "CH-AUDIT-01",
                "description", "Portão Principal",
                "keyCategory", "COMMON_AREA"
        );
        mockMvc.perform(post("/condominiums/" + condo1.getId() + "/keys")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keyBody)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/condominiums/" + condo1.getId() + "/keys")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keyBody)))
                .andExpect(status().isConflict());

        // Medição cross-condomínio -> 400 Bad Request
        Map<String, Object> crossUtility = Map.of(
                "unitId", unit201.getId(),
                "year", 2026,
                "month", 6,
                "utilityType", "WATER",
                "currentReading", 150.5,
                "previousReading", 140.0
        );
        mockMvc.perform(post("/condominiums/" + condo1.getId() + "/utility-readings")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossUtility)))
                .andExpect(status().isBadRequest());

        // Passe digital de visitante restrito ao anfitrião/unidade ou staff -> outro morador recebe 403 Forbidden
        VisitorAccess visitor = new VisitorAccess();
        visitor.setCondominium(condo1);
        visitor.setUnit(unit101);
        visitor.setResident(residentUser);
        visitor.setVisitorName("Visitante Autorizado");
        visitor.setAccessCode("PASS1234");
        visitor = visitorAccessRepository.save(visitor);

        mockMvc.perform(get("/visitors/" + visitor.getId() + "/digital-pass")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/visitors/" + visitor.getId() + "/digital-pass")
                        .header("Authorization", "Bearer " + otherResidentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoormanChatFindMessagesMarksResidentMessageReadWhenStaffReads() throws Exception {
        String residentToken = login(residentUser.getEmail());
        String managerToken = login(managerUser.getEmail());

        String sentJson = mockMvc.perform(post("/condominiums/" + condo1.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "Portaria, pode liberar meu entregador?"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long msgId = objectMapper.readTree(sentJson).get("id").asLong();
        DoormanChatMessage beforeRead = chatMessageRepository.findById(msgId).orElseThrow();
        assertFalse(beforeRead.getIsRead());

        mockMvc.perform(get("/condominiums/" + condo1.getId() + "/doorman-chat/messages")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("targetUserId", residentUser.getId().toString()))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        DoormanChatMessage afterRead = chatMessageRepository.findById(msgId).orElseThrow();
        assertTrue(afterRead.getIsRead(), "Leitura pela equipe deve marcar mensagem do morador como lida");
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

    private void saveActiveMembership(User user, Condominium condo, Role role, Unit unit) {
        Membership m = new Membership();
        m.setUser(user);
        m.setCondominium(condo);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setRequestedUnit(unit);
        membershipRepository.save(m);
    }

    private void saveResidentLink(User user, Unit unit) {
        Resident r = new Resident();
        r.setUser(user);
        r.setUnit(unit);
        r.setType(ResidentType.OWNER);
        residentRepository.save(r);
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
