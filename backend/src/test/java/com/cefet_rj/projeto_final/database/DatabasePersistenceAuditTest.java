package com.cefet_rj.projeto_final.database;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.community.entities.KeyRegistryEntry;
import com.cefet_rj.projeto_final.community.entities.ParkingSpot;
import com.cefet_rj.projeto_final.community.repositories.KeyRegistryEntryRepository;
import com.cefet_rj.projeto_final.community.repositories.ParkingSpotRepository;
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
import com.cefet_rj.projeto_final.reservations.dtos.ReservationRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.reservations.services.ReservationService;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "security.client-id=test-client",
                "security.client-secret=test-secret"
        }
)
@ActiveProfiles("test")
public class DatabasePersistenceAuditTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ResidentRepository residentRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private CommonAreaRepository commonAreaRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationService reservationService;
    @Autowired private ParkingSpotRepository parkingSpotRepository;
    @Autowired private KeyRegistryEntryRepository keyRegistryEntryRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private Condominium testCondo;
    private Sector testSector;
    private Unit testUnitA;
    private Unit testUnitB;
    private User testUser1;
    private User testUser2;
    private Role residentRole;
    private Role managerRole;
    private CommonArea testCommonArea;

    @BeforeEach
    void setUp() {
        residentRole = getOrCreateRole("ROLE_RESIDENT");
        managerRole = getOrCreateRole("ROLE_MANAGER");

        Feature resFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        BillingPlan plan = billingPlanRepository.findByNameIgnoreCase("AuditPersistencePlan")
                .orElseGet(() -> {
                    BillingPlan p = new BillingPlan();
                    p.setName("AuditPersistencePlan");
                    p.setMonthlyFee(BigDecimal.valueOf(99.00));
                    p.setActive(true);
                    p.getFeatures().add(resFeature);
                    return billingPlanRepository.save(p);
                });

        testCondo = new Condominium();
        testCondo.setName("Condomínio Audit DB " + UUID.randomUUID().toString().substring(0, 8));
        testCondo.setAddress("Rua Teste Persistencia, 100");
        testCondo.setBillingPlan(plan);
        testCondo.setIsActive(true);
        testCondo.setPlanSelected(true);
        testCondo = condominiumRepository.save(testCondo);

        testSector = new Sector();
        testSector.setName("Bloco A");
        testSector.setStructuralType(SectorStructuralType.BLOCK);
        testSector.setCondominium(testCondo);
        testSector = sectorRepository.save(testSector);

        testUnitA = new Unit();
        testUnitA.setIdentifier("101");
        testUnitA.setSector(testSector);
        testUnitA = unitRepository.save(testUnitA);

        testUnitB = new Unit();
        testUnitB.setIdentifier("102");
        testUnitB.setSector(testSector);
        testUnitB = unitRepository.save(testUnitB);

        testUser1 = createUser("audit_user1_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com");
        testUser2 = createUser("audit_user2_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com");

        testCommonArea = new CommonArea();
        testCommonArea.setName("Salão de Festas Audit");
        testCommonArea.setCapacity(50);
        testCommonArea.setActive(true);
        testCommonArea.setRequiresApproval(false);
        testCommonArea.setCondominium(testCondo);
        testCommonArea = commonAreaRepository.save(testCommonArea);
    }

    private User createUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("Audit");
        u.setLastName("Tester");
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.getRoles().add(residentRole);
        return userRepository.save(u);
    }

    private Role getOrCreateRole(String authority) {
        Role r = roleRepository.findByAuthority(authority);
        if (r != null) {
            return r;
        }
        r = new Role();
        r.setAuthority(authority);
        return roleRepository.save(r);
    }

    private Feature getOrCreateFeature(FeatureCode code) {
        return featureRepository.findByCode(code.name())
                .orElseGet(() -> {
                    Feature f = new Feature();
                    f.setCode(code.name());
                    f.setName(code.name());
                    f.setDescription("Feature for testing");
                    return featureRepository.save(f);
                });
    }

    @Test
    @DisplayName("Referential Integrity: Duplicate Membership for same user, condominium, and role is rejected")
    void testMembershipUniquenessConstraint() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // First membership creation succeeds
        template.execute(status -> {
            Membership m1 = new Membership();
            m1.setUser(testUser1);
            m1.setCondominium(testCondo);
            m1.setRole(residentRole);
            m1.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.saveAndFlush(m1);
            return null;
        });

        // Duplicate membership creation must violate unique constraint and throw exception
        assertThrows(DataIntegrityViolationException.class, () -> {
            template.execute(status -> {
                Membership m2 = new Membership();
                m2.setUser(testUser1);
                m2.setCondominium(testCondo);
                m2.setRole(residentRole);
                m2.setStatus(MembershipStatus.ACTIVE);
                membershipRepository.saveAndFlush(m2);
                return null;
            });
        });
    }

    @Test
    @DisplayName("Referential Integrity: Duplicate Resident association for same user and unit is rejected")
    void testResidentUniquenessConstraint() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // First resident association succeeds
        template.execute(status -> {
            Resident r1 = new Resident();
            r1.setUser(testUser1);
            r1.setUnit(testUnitA);
            r1.setType(ResidentType.OWNER);
            residentRepository.saveAndFlush(r1);
            return null;
        });

        // Duplicate resident association to the same unit must violate unique constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            template.execute(status -> {
                Resident r2 = new Resident();
                r2.setUser(testUser1);
                r2.setUnit(testUnitA);
                r2.setType(ResidentType.DEPENDENT);
                residentRepository.saveAndFlush(r2);
                return null;
            });
        });
    }

    @Test
    @DisplayName("Referential Integrity: Duplicate Parking Spot code in same condominium is rejected")
    void testParkingSpotUniquenessConstraint() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        template.execute(status -> {
            ParkingSpot p1 = new ParkingSpot();
            p1.setCondominium(testCondo);
            p1.setCode("VAGA-01");
            parkingSpotRepository.saveAndFlush(p1);
            return null;
        });

        assertThrows(DataIntegrityViolationException.class, () -> {
            template.execute(status -> {
                ParkingSpot p2 = new ParkingSpot();
                p2.setCondominium(testCondo);
                p2.setCode("VAGA-01");
                parkingSpotRepository.saveAndFlush(p2);
                return null;
            });
        });
    }

    @Test
    @DisplayName("Referential Integrity: Duplicate Key Registry code in same condominium is rejected")
    void testKeyRegistryUniquenessConstraint() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        template.execute(status -> {
            KeyRegistryEntry k1 = new KeyRegistryEntry();
            k1.setCondominium(testCondo);
            k1.setKeyCode("KEY-SALON-01");
            k1.setDescription("Chave Principal do Salão");
            keyRegistryEntryRepository.saveAndFlush(k1);
            return null;
        });

        assertThrows(DataIntegrityViolationException.class, () -> {
            template.execute(status -> {
                KeyRegistryEntry k2 = new KeyRegistryEntry();
                k2.setCondominium(testCondo);
                k2.setKeyCode("KEY-SALON-01");
                k2.setDescription("Chave Duplicada Proibida");
                keyRegistryEntryRepository.saveAndFlush(k2);
                return null;
            });
        });
    }

    @Test
    @DisplayName("N+1 Query Elimination: Single query aggregation correctly counts residents per unit")
    void testCountResidentsByUnitAggregation() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        template.execute(status -> {
            Resident r1 = new Resident();
            r1.setUser(testUser1);
            r1.setUnit(testUnitA);
            r1.setType(ResidentType.OWNER);
            residentRepository.save(r1);

            Resident r2 = new Resident();
            r2.setUser(testUser2);
            r2.setUnit(testUnitA);
            r2.setType(ResidentType.DEPENDENT);
            residentRepository.save(r2);
            return null;
        });

        List<Object[]> counts = residentRepository.countResidentsByUnitForCondominium(testCondo.getId());
        assertNotNull(counts);
        assertFalse(counts.isEmpty());

        Object[] unitARow = counts.stream()
                .filter(row -> testUnitA.getId().equals(row[0]))
                .findFirst()
                .orElse(null);

        assertNotNull(unitARow);
        assertEquals(2L, ((Number) unitARow[1]).longValue(), "Unit 101 should have exactly 2 residents in aggregated count");
    }

    @Test
    @DisplayName("EntityGraph Pagination: Membership and Resident paged queries fetch associations cleanly")
    void testEntityGraphPagedQueries() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        template.execute(status -> {
            Membership m = new Membership();
            m.setUser(testUser1);
            m.setCondominium(testCondo);
            m.setRole(residentRole);
            m.setStatus(MembershipStatus.ACTIVE);
            m.setRequestedUnit(testUnitA);
            membershipRepository.save(m);

            Resident r = new Resident();
            r.setUser(testUser1);
            r.setUnit(testUnitA);
            r.setType(ResidentType.OWNER);
            residentRepository.save(r);
            return null;
        });

        Page<Membership> pagedMemberships = membershipRepository.findAll(PageRequest.of(0, 10));
        assertNotNull(pagedMemberships);
        assertFalse(pagedMemberships.isEmpty());
        // Verify associations are loaded without throwing LazyInitializationException
        pagedMemberships.forEach(m -> {
            assertNotNull(m.getUser().getEmail());
            assertNotNull(m.getCondominium().getName());
            assertNotNull(m.getRole().getAuthority());
        });

        Page<Resident> pagedResidents = residentRepository.findAll(PageRequest.of(0, 10));
        assertNotNull(pagedResidents);
        assertFalse(pagedResidents.isEmpty());
        pagedResidents.forEach(r -> {
            assertNotNull(r.getUser().getEmail());
            assertNotNull(r.getUnit().getIdentifier());
        });
    }

    @Test
    @DisplayName("Transaction Rollback: Unsuccessful operations do not leave partial or corrupt data")
    void testTransactionRollbackOnFailure() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        long initialMembershipCount = membershipRepository.count();

        assertThrows(RuntimeException.class, () -> {
            template.execute(status -> {
                Membership m = new Membership();
                m.setUser(testUser2);
                m.setCondominium(testCondo);
                m.setRole(residentRole);
                m.setStatus(MembershipStatus.ACTIVE);
                membershipRepository.save(m);

                // Simulate business rule failure after first save
                throw new RuntimeException("Simulated transaction failure");
            });
        });

        long finalMembershipCount = membershipRepository.count();
        assertEquals(initialMembershipCount, finalMembershipCount, "Aborted transaction must roll back completely without partial state");
    }

    @Test
    @DisplayName("Pessimistic Lock & Concurrency: Prevents double-booking same slot under concurrent attempts")
    void testConcurrencyDoubleBookingPrevention() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Set up active memberships for both users
        template.execute(status -> {
            Membership m1 = new Membership();
            m1.setUser(testUser1);
            m1.setCondominium(testCondo);
            m1.setRole(residentRole);
            m1.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.save(m1);

            Membership m2 = new Membership();
            m2.setUser(testUser2);
            m2.setCondominium(testCondo);
            m2.setRole(residentRole);
            m2.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.save(m2);
            return null;
        });

        LocalDateTime slotStart = LocalDateTime.now().plusDays(20).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slotEnd = slotStart.plusHours(4);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            try {
                barrier.await();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testUser1.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")))
                );
                reservationService.create(testCommonArea.getId(), new ReservationRequestDTO(slotStart, slotEnd, testUser1.getId(), "11999990001", "Festa 1", false));
                confirmedCount.incrementAndGet();
            } catch (Exception e) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            try {
                barrier.await();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testUser2.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")))
                );
                reservationService.create(testCommonArea.getId(), new ReservationRequestDTO(slotStart, slotEnd, testUser2.getId(), "11999990002", "Festa 2", false));
                confirmedCount.incrementAndGet();
            } catch (Exception e) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        List<Future<Void>> futures = executor.invokeAll(List.of(task1, task2));
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Exactly ONE booking must succeed and the competing one must fail (no double booking)
        assertEquals(1, confirmedCount.get(), "Exactly one reservation must be confirmed for the exact same slot");
        assertEquals(1, conflictCount.get(), "The competing reservation must encounter a conflict exception");
    }
}
