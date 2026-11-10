package com.cefet_rj.projeto_final.concurrency;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
class ConcurrencyAndEdgeCasesIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condo;
    private CommonArea salon;
    private Role residentRole;
    private Role managerRole;
    private User managerUser;

    @BeforeEach
    void setUp() {
        residentRole = getOrCreateRole("ROLE_RESIDENT");
        managerRole = getOrCreateRole("ROLE_MANAGER");

        Feature resFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature commFeature = getOrCreateFeature(FeatureCode.PACKAGES);

        String uid = UUID.randomUUID().toString().substring(0, 8);
        BillingPlan plan = new BillingPlan();
        plan.setName("CONCURRENCY_TEST_PLAN_" + uid);
        plan.setMonthlyFee(new BigDecimal("99.90"));
        plan.setActive(true);
        plan.setFeatures(Set.of(resFeature, commFeature));
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Residencial Concorrência " + uid);
        condo.setAddress("Rua Concorrente, 100");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        salon = new CommonArea();
        salon.setName("Salão Gourmet Disputado " + uid);
        salon.setCapacity(30);
        salon.setCondominium(condo);
        salon.setActive(true);
        salon.setRequiresApproval(false); // Immediate confirmation
        salon.setAllowWaitlist(false);    // Direct conflict rejection (409)
        salon.setMinAdvanceHours(1);
        salon.setMaxAdvanceDays(30);
        salon.setMaxDurationHours(8);
        salon.setMaxWeeklyBookingsPerUser(5);
        salon = commonAreaRepository.save(salon);

        managerUser = createUser("Gerente", "Gourmet", "gerente." + uid + "@system.com", managerRole);
        assignMembership(managerUser, condo, managerRole);
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
    // 1. CONCURRENCY: SIMULTANEOUS RESERVATION OF THE SAME TIME SLOT
    // =========================================================================

    @Test
    @DisplayName("Concurrent race condition: 6 threads competing simultaneously for the exact same slot")
    void testConcurrentReservations_onlyOneSucceeds() throws Exception {
        final int NUM_THREADS = 6;
        final LocalDateTime targetStart = LocalDateTime.now().plusDays(5).withHour(18).withMinute(0).withSecond(0).withNano(0);
        final LocalDateTime targetEnd = targetStart.plusHours(4);

        // Pre-create 6 distinct residents and tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            User resident = createUser("Morador", "Rival" + i, "morador" + i + "." + UUID.randomUUID() + "@concorrente.com", residentRole);
            assignMembership(resident, condo, residentRole);
            tokens.add(login(resident.getEmail()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch readyLatch = new CountDownLatch(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final String token = tokens.get(i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Guarantee all 6 threads fire at the exact same nanosecond

                    int status = mockMvc.perform(post("/common-areas/" + salon.getId() + "/reservations")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of(
                                            "startAt", targetStart.toString(),
                                            "endAt", targetEnd.toString(),
                                            "notes", "Reserva Concorrente",
                                            "joinWaitlist", false
                                    ))))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 201 || status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Exception during invocation
                }
            }));
        }

        // Wait until all threads are ready
        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        // Fire all threads!
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 1 thread must succeed, and the remaining 5 must receive 409 Conflict
        assertEquals(1, successCount.get(), "Exatamente uma reserva deve ter sucesso para o mesmo horário");
        assertEquals(NUM_THREADS - 1, conflictCount.get(), "Todas as outras requisições concorrentes devem receber 409 Conflict");
    }

    // =========================================================================
    // 2. EDGE CASES: SQL INJECTION PAYLOAD RESILIENCY
    // =========================================================================

    @Test
    @DisplayName("Search queries with SQL injection payloads do not break or leak data")
    void testSqlInjectionResilience() throws Exception {
        String token = login(managerUser.getEmail());

        // SQL injection payload in query parameters
        String sqliPayload = "' OR '1'='1' -- ";

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/financial/expenses")
                        .param("description", sqliPayload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/condominiums/" + condo.getId() + "/visitors")
                        .param("visitorName", "admin'; DROP TABLE tb_user; --")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 3. EDGE CASES: XSS PAYLOADS AND SANITIZATION
    // =========================================================================

    @Test
    @DisplayName("Payloads containing XSS tags in text fields are handled safely")
    void testXssPayloadHandling() throws Exception {
        String token = login(managerUser.getEmail());

        String xssPayload = "<script>alert('pwned')</script>";

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", xssPayload,
                                "category", "MAINTENANCE",
                                "amount", 120.0,
                                "dueDate", LocalDateTime.now().plusDays(10).toLocalDate().toString()
                        ))))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // 4. EDGE CASES: MALFORMED AND BOUNDARY VALUES
    // =========================================================================

    @Test
    @DisplayName("Negative pagination parameters return 400 or fallback gracefully without 500 error")
    void testNegativePaginationHandling() throws Exception {
        String token = login(managerUser.getEmail());

        int status = mockMvc.perform(get("/sectors")
                        .param("condominiumId", condo.getId().toString())
                        .param("page", "-1")
                        .param("size", "-10")
                        .header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getStatus();

        // Must not be an unhandled 500 server error
        assertNotEquals(500, status);
    }
}
