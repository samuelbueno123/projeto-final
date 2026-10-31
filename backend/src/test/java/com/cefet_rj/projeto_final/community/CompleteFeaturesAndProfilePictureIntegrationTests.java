package com.cefet_rj.projeto_final.community;

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
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
                "security.client-secret=test-secret",
                "app.media.pictures-dir=media/pictures"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompleteFeaturesAndProfilePictureIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";
    // Valid 1x1 PNG in base64
    private static final String SAMPLE_PNG_BASE64 =
            Base64.getEncoder().encodeToString(new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
            });

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
    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private PictureStorageService pictureStorageService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condo;
    private Unit unit101;
    private CommonArea partyRoom;
    private User managerUser;
    private User residentUser;
    private User newCandidateUser;

    @BeforeEach
    void setUp() {
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role doormanRole = getOrCreateRole("ROLE_DOORMAN");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES);
        Feature announcementsFeature = getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);
        Feature notificationsFeature = getOrCreateFeature(FeatureCode.NOTIFICATIONS);

        BillingPlan plan = new BillingPlan();
        plan.setName("FULL_SUITE_TEST_PLAN");
        plan.setMonthlyFee(new BigDecimal("299.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, packagesFeature, announcementsFeature, notificationsFeature));
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Condomínio Solar das Palmeiras");
        condo.setAddress("Av. Central, 1000");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        Sector sector = new Sector();
        sector.setName("Bloco A");
        sector.setStructuralType(SectorStructuralType.BLOCK);
        sector.setCondominium(condo);
        sector = sectorRepository.save(sector);

        unit101 = new Unit();
        unit101.setIdentifier("101");
        unit101.setSector(sector);
        unit101 = unitRepository.save(unit101);

        partyRoom = new CommonArea();
        partyRoom.setName("Salão de Festas Gourmet");
        partyRoom.setDescription("Espaço climatizado para eventos");
        partyRoom.setCapacity(60);
        partyRoom.setActive(true);
        partyRoom.setRequiresApproval(true);
        partyRoom.setOpeningHour(8);
        partyRoom.setClosingHour(22);
        partyRoom.setCondominium(condo);
        partyRoom = commonAreaRepository.save(partyRoom);

        managerUser = createUser("Roberto", "Síndico", "roberto.manager@test.com", managerRole);
        residentUser = createUser("Helena", "Moradora", "helena.resident@test.com", residentRole);
        newCandidateUser = createUser("Lucas", "Novo", "lucas.novo@test.com", residentRole);

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
    void profilePictureStoresPathInDatabaseAndFileInMediaPicturesAndServesViaHttp() throws Exception {
        String residentToken = login(residentUser.getEmail());

        String dataUrl = "data:image/png;base64," + SAMPLE_PNG_BASE64;
        String profileResp = mockMvc.perform(put("/users/me/profile")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Helena",
                                "lastName", "Moradora",
                                "email", residentUser.getEmail(),
                                "profileImage", dataUrl
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String storedPath = objectMapper.readTree(profileResp).get("profileImage").asText();
        assertTrue(storedPath.startsWith("media/pictures/"), "DB must store path starting with media/pictures/");
        assertFalse(storedPath.startsWith("data:image/"), "DB must NOT store raw base64 data");

        User dbUser = userRepository.findById(residentUser.getId()).orElseThrow();
        assertEquals(storedPath, dbUser.getProfileImage());

        String fileName = storedPath.substring("media/pictures/".length());
        Path diskFile = pictureStorageService.getStorageDirectory().resolve(fileName);
        assertTrue(Files.exists(diskFile), "File must exist on server disk under media/pictures");

        // Verify GET /media/pictures/{filename} serves the image bytes publicly
        mockMvc.perform(get("/" + storedPath))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        // Verify multipart upload endpoint POST /users/me/profile/picture also works and replaces old file
        byte[] rawBytes = Base64.getDecoder().decode(SAMPLE_PNG_BASE64);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                rawBytes
        );

        String uploadResp = mockMvc.perform(multipart("/users/me/profile/picture")
                        .file(multipartFile)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondPath = objectMapper.readTree(uploadResp).get("profileImage").asText();
        assertTrue(secondPath.startsWith("media/pictures/"));
        assertNotEquals(storedPath, secondPath);

        // Clean up test file
        pictureStorageService.deleteFileIfPresent(secondPath);
    }

    @Test
    void reservationAvailabilityCheckAndManagerApprovalFlowAndUnitJoinRequestFlow() throws Exception {
        String residentToken = login(residentUser.getEmail());
        String managerToken = login(managerUser.getEmail());
        String candidateToken = login(newCandidateUser.getEmail());

        LocalDate targetDate = LocalDate.now().plusDays(3);
        LocalDateTime startAt = targetDate.atTime(14, 0);
        LocalDateTime endAt = targetDate.atTime(18, 0);

        // 1. Check availability before requesting reservation
        mockMvc.perform(get("/common-areas/" + partyRoom.getId() + "/check-availability")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("startAt", startAt.toString())
                        .param("endAt", endAt.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.requiresApproval").value(true));

        // 2. Resident requests reservation -> created as PENDING because requiresApproval=true
        String resCreated = mockMvc.perform(post("/common-areas/" + partyRoom.getId() + "/reservations")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startAt", startAt.toString(),
                                "endAt", endAt.toString(),
                                "contactPhone", "21999998888",
                                "notes", "Aniversário familiar"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("Aniversário familiar"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long reservationId = objectMapper.readTree(resCreated).get("id").asLong();

        // 3. Manager approves the reservation -> becomes CONFIRMED
        mockMvc.perform(patch("/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 4. Candidate checks available condominiums and units, then sends a join request
        mockMvc.perform(get("/condominiums/available")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(condo.getId()));

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/available-units")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(unit101.getId()));

        String joinResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/membership-requests")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "authority", "ROLE_RESIDENT",
                                "unitId", unit101.getId(),
                                "residentType", "TENANT",
                                "message", "Acabei de assinar o contrato de locação da unidade 101"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.unitId").value(unit101.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long membershipRequestId = objectMapper.readTree(joinResp).get("id").asLong();

        // 5. Manager approves the join request -> status ACTIVE and Resident linked to unit 101
        mockMvc.perform(post("/manager/condominiums/" + condo.getId() + "/membership-requests/" + membershipRequestId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.unitId").value(unit101.getId()));
    }

    @Test
    void nineCondominiumFeaturesAndPaymentConfirmationWorkflowWorkEndToEnd() throws Exception {
        String residentToken = login(residentUser.getEmail());
        String managerToken = login(managerUser.getEmail());

        // Feature 0: Resident requests payment confirmation for Unit 101
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/units/" + unit101.getId() + "/request-payment-confirmation")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("note", "Comprovante PIX #998877"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMATION_REQUESTED"));

        // Feature 1: Occurrence Ticket
        String ticketResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/tickets")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "MAINTENANCE",
                                "title", "Lâmpada queimada no corredor",
                                "description", "O 1º andar está com sensor de presença piscando.",
                                "location", "Bloco A - 1º Andar"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long ticketId = objectMapper.readTree(ticketResp).get("id").asLong();

        mockMvc.perform(patch("/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED",
                                "adminResponse", "Lâmpada substituída pela equipe de manutenção."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Feature 2: Visitor Access & Pre-Authorization
        String visitorResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/visitors")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "visitorName", "Técnico Fibra Óptica",
                                "documentNumber", "123.456.789-00",
                                "visitorType", "SERVICE_PROVIDER",
                                "vehiclePlate", "ABC1D23"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.accessCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long visitorId = objectMapper.readTree(visitorResp).get("id").asLong();

        mockMvc.perform(patch("/visitors/" + visitorId + "/check-in")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        // Feature 3: Polls & Virtual Assembly Voting
        String pollResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/polls")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Modernização da Academia",
                                "description", "Aprovação do orçamento de novos equipamentos",
                                "options", List.of("Aprovar orçamento A", "Aprovar orçamento B", "Adiar para o próximo semestre")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        JsonNode pollJson = objectMapper.readTree(pollResp);
        long pollId = pollJson.get("id").asLong();
        long firstOptionId = pollJson.get("options").get(0).get("id").asLong();

        mockMvc.perform(post("/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", firstOptionId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1))
                .andExpect(jsonPath("$.myOptionId").value(firstOptionId));

        // Feature 4: Lost and Found
        String lostResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/lost-found")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "FOUND",
                                "title", "Chaveiro com tag azul",
                                "description", "Encontrado próximo à piscina",
                                "location", "Portaria Central"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long lostId = objectMapper.readTree(lostResp).get("id").asLong();

        mockMvc.perform(patch("/lost-found/" + lostId + "/request-claim")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIM_REQUESTED"));

        // Feature 5: Classifieds & Neighbor Services
        String adResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/classifieds")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "PARKING_RENTAL",
                                "title", "Alugo vaga coberta G-12",
                                "description", "Disponível durante horário comercial ou mensal",
                                "price", "250.00",
                                "contactPhone", "21988887777"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long adId = objectMapper.readTree(adResp).get("id").asLong();

        mockMvc.perform(post("/classifieds/" + adId + "/interest")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Tenho interesse em alugar mensalmente!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestCount").value(1));

        // Feature 6: Vehicles & Plate Check
        String vehResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CAR",
                                "brandModel", "Toyota Corolla Cross",
                                "color", "Branco",
                                "licensePlate", "RJA9E88",
                                "parkingSpot", "Vaga 101-A"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long vehicleId = objectMapper.readTree(vehResp).get("id").asLong();

        mockMvc.perform(patch("/vehicles/" + vehicleId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/vehicles/check-plate")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("plate", "RJA9E88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.residentVehicles.length()").value(1));

        // Feature 7: Pet Registry
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/pets")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Thor",
                                "species", "DOG",
                                "breed", "Golden Retriever",
                                "size", "LARGE",
                                "vaccinated", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Thor"));

        // Feature 8: Moving Schedule & Availability Check
        LocalDate moveDate = LocalDate.now().plusDays(5);
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/moving-requests/check-availability")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("date", moveDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morningAvailable").value(true));

        String moveResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/moving-requests")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "LARGE_DELIVERY",
                                "movingDate", moveDate.toString(),
                                "period", "MORNING",
                                "companyName", "Transportadora Rápida",
                                "notes", "Entrega de sofá e geladeira pelo elevador de serviço"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long moveId = objectMapper.readTree(moveResp).get("id").asLong();

        mockMvc.perform(patch("/moving-requests/" + moveId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Feature 9: Condominium Documents & Emergency Contacts
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/documents")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "INTERNAL_RULES",
                                "title", "Regimento Interno Consolidado 2026",
                                "summary", "Horários de silêncio, uso da piscina e salão de festas",
                                "contentOrUrl", "Horário de silêncio das 22h às 08h. Obras permitidas de seg a sex das 08h às 17h.",
                                "pinned", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinned").value(true));

        // Feature 10: Smart Lockers & Compartment Retrieval
        String lockerResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/lockers")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "compartmentNumber", "L-12",
                                "unitId", unit101.getId(),
                                "trackingCode", "BR987654321AM",
                                "courierName", "Amazon Prime"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.pinCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode lockerJson = objectMapper.readTree(lockerResp);
        long lockerId = lockerJson.get("id").asLong();
        String lockerPin = lockerJson.get("pinCode").asText();

        mockMvc.perform(patch("/lockers/" + lockerId + "/retrieve")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinCode", lockerPin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRIEVED"));

        // Feature 11: Event Guest List & Fast Reception Check-In
        String guestListResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/guest-lists")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventTitle", "Festa de Aniversário 101",
                                "eventDate", LocalDate.now().plusDays(2).toString(),
                                "unitId", unit101.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventTitle").value("Festa de Aniversário 101"))
                .andReturn().getResponse().getContentAsString();

        long guestListId = objectMapper.readTree(guestListResp).get("id").asLong();

        String addGuestResp = mockMvc.perform(post("/guest-lists/" + guestListId + "/guests")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Carlos Drummond",
                                "documentNumber", "111.222.333-44"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long guestEntryId = objectMapper.readTree(addGuestResp).get("guests").get(0).get("id").asLong();

        mockMvc.perform(patch("/guests/" + guestEntryId + "/check-in")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests[0].checkedIn").value(true));

        // Feature 12: Infractions, Notification Defense & Judgment
        String infractionResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/infractions")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "type", "WARNING",
                                "title", "Ruído excessivo após às 22h",
                                "description", "Música audível na área externa além do horário permitido",
                                "fineAmount", "0.00"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn().getResponse().getContentAsString();

        long infractionId = objectMapper.readTree(infractionResp).get("id").asLong();

        mockMvc.perform(post("/infractions/" + infractionId + "/defense")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "defense", "O barulho vinha do apartamento ao lado conforme informado no livro de portaria."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEFENSE_SUBMITTED"));

        mockMvc.perform(patch("/infractions/" + infractionId + "/judge")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CANCELLED",
                                "judgment", "Defesa acatada após conferência dos registros da portaria."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private Role getOrCreateRole(String authority) {
        Role role = roleRepository.findByAuthority(authority);
        if (role == null) {
            role = new Role();
            role.setAuthority(authority);
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
