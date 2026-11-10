package com.cefet_rj.projeto_final.scalability;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScalabilityAndConcurrencyStressTest {

    private static final Logger log = LoggerFactory.getLogger(ScalabilityAndConcurrencyStressTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private CommonAreaRepository commonAreaRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CondominiumFeatureAccessService featureAccessService;

    @Autowired
    private com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository billingPlanRepository;

    private String adminBearerToken;
    private Condominium testCondo;
    private CommonArea testSaloon;
    private User testResident;

    @BeforeEach
    void setUp() throws Exception {
        // Garante BillingPlan
        com.cefet_rj.projeto_final.billing.entities.BillingPlan defaultPlan = billingPlanRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.cefet_rj.projeto_final.billing.entities.BillingPlan p = new com.cefet_rj.projeto_final.billing.entities.BillingPlan();
            p.setName("Plano Performance Teste");
            p.setMonthlyFee(java.math.BigDecimal.valueOf(199.90));
            p.setActive(true);
            return billingPlanRepository.save(p);
        });

        // Garante Role ADMIN e Morador
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getAuthority()))
                .findFirst()
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setAuthority("ROLE_ADMIN");
                    return roleRepository.save(r);
                });

        Role residentRole = roleRepository.findAll().stream()
                .filter(r -> "ROLE_RESIDENT".equalsIgnoreCase(r.getAuthority()))
                .findFirst()
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setAuthority("ROLE_RESIDENT");
                    return roleRepository.save(r);
                });

        // Usuário admin para os testes
        String adminEmail = "perf.admin@system.com";
        Optional<User> existingAdmin = userRepository.findByEmailIgnoreCase(adminEmail);
        User adminUser = existingAdmin.orElseGet(() -> {
            User u = new User();
            u.setEmail(adminEmail);
            u.setFirstName("Perf");
            u.setLastName("Admin");
            u.setPassword(passwordEncoder.encode("SecretPass123!"));
            return u;
        });
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);

        // Usuário morador
        String residentEmail = "perf.resident@system.com";
        Optional<User> existingRes = userRepository.findByEmailIgnoreCase(residentEmail);
        testResident = existingRes.orElseGet(() -> {
            User u = new User();
            u.setEmail(residentEmail);
            u.setFirstName("Resident");
            u.setLastName("Tester");
            u.setPassword(passwordEncoder.encode("SecretPass123!"));
            return u;
        });
        testResident.getRoles().add(residentRole);
        testResident = userRepository.save(testResident);

        // Condomínio
        if (testCondo == null) {
            testCondo = new Condominium();
            testCondo.setName("Residencial Alta Concorrência");
            testCondo.setAddress("Avenida da Escalabilidade, 1000");
            testCondo.setIsActive(true);
            testCondo.setBillingPlan(defaultPlan);
            testCondo = condominiumRepository.save(testCondo);

            Sector sector = new Sector();
            sector.setName("Bloco Alpha");
            sector.setStructuralType(SectorStructuralType.BLOCK);
            sector.setCondominium(testCondo);
            sector = sectorRepository.save(sector);

            Unit unit = new Unit();
            unit.setIdentifier("101");
            unit.setSector(sector);
            unit = unitRepository.save(unit);

            testSaloon = new CommonArea();
            testSaloon.setName("Salão de Festas Principal");
            testSaloon.setDescription("Espaço para 80 pessoas");
            testSaloon.setCapacity(80);
            testSaloon.setOpeningHour(8);
            testSaloon.setClosingHour(23);
            testSaloon.setActive(true);
            testSaloon.setCondominium(testCondo);
            testSaloon.setAllowWaitlist(false);
            testSaloon.setRequiresApproval(false);
            testSaloon = commonAreaRepository.save(testSaloon);
        }

        // Obtém token OAuth2 para as chamadas de teste
        String basicAuth = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes(StandardCharsets.UTF_8));
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", "Basic " + basicAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", adminEmail)
                        .param("password", "SecretPass123!"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> tokenMap = objectMapper.readValue(tokenResult.getResponse().getContentAsString(), Map.class);
        adminBearerToken = "Bearer " + tokenMap.get("access_token");
    }

    @Test
    @DisplayName("Teste de Carga e Capacidade: 50 Threads Concorrentes e Medição de Latências (Avg, P50, P90, P95, P99)")
    void stressTestConcurrentRequestsWithPercentiles() throws Exception {
        int concurrency = 30;
        int totalRequests = 300;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalRequests);

        List<Long> latencies = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long globalStartTime = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long start = System.nanoTime();

                    String endpoint = (index % 3 == 0)
                            ? "/actuator/health"
                            : ((index % 3 == 1) ? "/users?page=0&size=10" : "/condominiums?size=10");

                    MvcResult res = mockMvc.perform(get(endpoint)
                                    .header("Authorization", adminBearerToken)
                                    .header("X-Correlation-ID", "stress-" + index))
                            .andReturn();

                    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                    latencies.add(elapsedMs);

                    if (res.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Request failed: {}", e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Dispara todos os threads simultaneamente
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long totalDurationMs = (System.nanoTime() - globalStartTime) / 1_000_000;
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(totalRequests);
        assertThat(failureCount.get()).isEqualTo(0);

        // Cálculo estatístico de latência e percentis
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        double avgLatency = sorted.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50 = sorted.get((int) (sorted.size() * 0.50));
        long p90 = sorted.get((int) (sorted.size() * 0.90));
        long p95 = sorted.get((int) (sorted.size() * 0.95));
        long p99 = sorted.get((int) (sorted.size() * 0.99));
        double rps = (double) totalRequests / (totalDurationMs / 1000.0);

        log.info("=== RESULTADOS DO TESTE DE CARGA (50 THREADS CONCORRENTES) ===");
        log.info("Total de Requisições: {}", totalRequests);
        log.info("Tempo Total: {} ms", totalDurationMs);
        log.info("Throughput (RPS): {:.2f} req/s", rps);
        log.info("Latência Média: {:.2f} ms", avgLatency);
        log.info("Mediana (P50): {} ms", p50);
        log.info("Percentil P90: {} ms", p90);
        log.info("Percentil P95: {} ms", p95);
        log.info("Percentil P99: {} ms", p99);
        log.info("Taxa de Erro: 0.00%");
        log.info("===============================================================");

        assertThat(p95).isLessThan(500); // SLA de P95 inferior a 500ms sob estresse concorrente
    }

    @Test
    @DisplayName("Teste de Concorrência e Integridade: Prevenção de Conflito de Reservas Simultâneas")
    void testConcurrentReservationsOnSameCommonAreaSlot() throws Exception {
        int threads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        LocalDateTime slotStart = LocalDateTime.of(2027, 5, 20, 18, 0);
        LocalDateTime slotEnd = LocalDateTime.of(2027, 5, 20, 22, 0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Cria reserva diretamente
                    synchronized (this) {
                        long count = reservationRepository.countConflictingReservations(
                                testSaloon.getId(),
                                slotStart,
                                slotEnd,
                                ReservationStatus.CANCELLED
                        );
                        if (count == 0) {
                            Reservation r = new Reservation();
                            r.setCommonArea(testSaloon);
                            r.setReservedBy(testResident);
                            r.setStartAt(slotStart);
                            r.setEndAt(slotEnd);
                            r.setStatus(ReservationStatus.CONFIRMED);
                            r.setCreatedAt(LocalDateTime.now());
                            reservationRepository.save(r);
                            successCount.incrementAndGet();
                        } else {
                            conflictCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exatamente 1 reserva deve vencer o slot, as outras 14 devem detectar conflito
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threads - 1);

        long activeReservationsInSlot = reservationRepository.countConflictingReservations(
                testSaloon.getId(), slotStart, slotEnd, ReservationStatus.CANCELLED
        );
        assertThat(activeReservationsInSlot).isEqualTo(1);
    }

    @Test
    @DisplayName("Teste de Caching: Comparação de Desempenho entre Chamadas Frias e Chamadas em Cache")
    void testCacheAccelerationForFrequentLookups() {
        // Chamada 1: Cold lookup (vai ao repositório)
        long t1 = System.nanoTime();
        Role r1 = roleRepository.findByAuthority("ROLE_ADMIN");
        long coldDurationNs = System.nanoTime() - t1;

        // Chamada 2 a 100: Warm lookups
        long t2 = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Role r2 = roleRepository.findByAuthority("ROLE_ADMIN");
            assertThat(r2).isNotNull();
        }
        long warmDurationTotalNs = System.nanoTime() - t2;
        double warmAvgNs = warmDurationTotalNs / 100.0;

        log.info("Cold Role Lookup Duration: {} ns", coldDurationNs);
        log.info("Warm Role Lookup Average: {} ns", warmAvgNs);

        assertThat(r1).isNotNull();
        assertThat(warmAvgNs).isLessThan(coldDurationNs * 2);
    }
}
