package com.cefet_rj.projeto_final.packages.services;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.history.entities.PackageHistory;
import com.cefet_rj.projeto_final.packages.history.repositories.PackageHistoryRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PackageServiceIntegrationTests {

    @Autowired
    private PackageService packageService;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageHistoryRepository packageHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private FeatureRepository featureRepository;

    private User admin;
    private User resident;
    private Package packageEntity;

    @BeforeEach
    void setUp() {

        notificationRepository.deleteAll();
        packageHistoryRepository.deleteAll();
        packageRepository.deleteAll();
        unitRepository.deleteAll();
        sectorRepository.deleteAll();
        condominiumRepository.deleteAll();
        billingPlanRepository.deleteAll();
        featureRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Feature packagesFeature = createFeature(
                FeatureCode.PACKAGES,
                "Encomendas"
        );

        Feature notificationsFeature = createFeature(
                FeatureCode.NOTIFICATIONS,
                "Notificações"
        );

        BillingPlan premiumPlan = createPlan(
                "PREMIUM",
                new BigDecimal("199.90"),
                Set.of(
                        packagesFeature,
                        notificationsFeature
                )
        );

        Condominium condominium = createCondominium(
                "Condomínio Premium",
                premiumPlan
        );

        Sector sector = createSector(condominium);

        Unit unit = createUnit(sector);

        Role adminRole = createRole("ROLE_ADMIN");

        admin = createUser(
                "Admin",
                "Sistema",
                "admin@test.com"
        );

        admin.addRole(adminRole);
        admin = userRepository.save(admin);

        resident = createUser(
                "Morador",
                "Premium",
                "resident@test.com"
        );

        packageEntity = createReceivedPackage(
                resident,
                unit
        );

        authenticate(admin);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notifyShouldChangeStatusCreateHistoryAndCreateNotification() {

        packageService.notify(packageEntity.getId());

        Package updatedPackage = packageRepository
                .findById(packageEntity.getId())
                .orElseThrow();

        assertEquals(
                PackageStatus.NOTIFIED,
                updatedPackage.getStatus()
        );

        assertNotNull(
                updatedPackage.getNotifiedAt()
        );

        List<PackageHistory> history =
                packageHistoryRepository
                        .findByPackageEntityIdOrderByPerformedAtAsc(
                                packageEntity.getId()
                        );

        assertEquals(
                1,
                history.size()
        );

        PackageHistory historyEntry = history.get(0);

        assertEquals(
                PackageStatus.RECEIVED,
                historyEntry.getPreviousStatus()
        );

        assertEquals(
                PackageStatus.NOTIFIED,
                historyEntry.getNewStatus()
        );

        assertEquals(
                admin.getId(),
                historyEntry.getPerformedBy().getId()
        );

        assertEquals(
                1,
                notificationRepository.count()
        );
    }

    @Test
    void basicPlanShouldNotifyPackageCreateHistoryButNotCreateNotification() {

        Feature packagesFeature = featureRepository
                .findByCode(FeatureCode.PACKAGES.name())
                .orElseThrow();

        BillingPlan basicPlan = createPlan(
                "BASIC_TEST",
                new BigDecimal("99.90"),
                Set.of(packagesFeature)
        );

        Condominium basicCondominium = createCondominium(
                "Condomínio Basic",
                basicPlan
        );

        Sector basicSector = createSector(
                basicCondominium
        );

        Unit basicUnit = createUnit(
                basicSector
        );

        User basicResident = createUser(
                "Morador",
                "Basic",
                "basic-resident@test.com"
        );

        Package basicPackage = createReceivedPackage(
                basicResident,
                basicUnit
        );

        long notificationsBefore =
                notificationRepository.count();

        packageService.notify(
                basicPackage.getId()
        );

        Package updatedPackage = packageRepository
                .findById(basicPackage.getId())
                .orElseThrow();

        assertEquals(
                PackageStatus.NOTIFIED,
                updatedPackage.getStatus()
        );

        assertNotNull(
                updatedPackage.getNotifiedAt()
        );

        List<PackageHistory> history =
                packageHistoryRepository
                        .findByPackageEntityIdOrderByPerformedAtAsc(
                                basicPackage.getId()
                        );

        assertEquals(
                1,
                history.size()
        );

        PackageHistory historyEntry = history.get(0);

        assertEquals(
                PackageStatus.RECEIVED,
                historyEntry.getPreviousStatus()
        );

        assertEquals(
                PackageStatus.NOTIFIED,
                historyEntry.getNewStatus()
        );

        assertEquals(
                admin.getId(),
                historyEntry.getPerformedBy().getId()
        );

        assertEquals(
                notificationsBefore,
                notificationRepository.count()
        );
    }

    private Feature createFeature(
            FeatureCode code,
            String name
    ) {
        Feature feature = new Feature();

        feature.setCode(code.name());
        feature.setName(name);
        feature.setDescription(
                "Feature de teste: " + name
        );

        return featureRepository.save(feature);
    }

    private BillingPlan createPlan(
            String name,
            BigDecimal monthlyFee,
            Set<Feature> features
    ) {
        BillingPlan plan = new BillingPlan();

        plan.setName(name);
        plan.setMonthlyFee(monthlyFee);
        plan.setActive(true);
        plan.setFeatures(
                new HashSet<>(features)
        );

        return billingPlanRepository.save(plan);
    }

    private Condominium createCondominium(
            String name,
            BillingPlan billingPlan
    ) {
        Condominium condominium = new Condominium();

        condominium.setName(name);
        condominium.setAddress(
                "Endereço de teste"
        );
        condominium.setBillingPlan(billingPlan);
        condominium.setIsActive(true);

        return condominiumRepository.save(condominium);
    }

    private Sector createSector(
            Condominium condominium
    ) {
        Sector sector = new Sector();

        sector.setName("Bloco A");
        sector.setStructuralType(
                SectorStructuralType.BLOCK
        );
        sector.setCondominium(condominium);

        return sectorRepository.save(sector);
    }

    private Unit createUnit(
            Sector sector
    ) {
        Unit unit = new Unit();

        unit.setIdentifier("101");
        unit.setSector(sector);

        return unitRepository.save(unit);
    }

    private Role createRole(
            String authority
    ) {
        Role role = new Role();

        role.setAuthority(authority);

        return roleRepository.save(role);
    }

    private User createUser(
            String firstName,
            String lastName,
            String email
    ) {
        User user = new User();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword("123456");

        return userRepository.save(user);
    }

    private Package createReceivedPackage(
            User recipient,
            Unit unit
    ) {
        Package packageEntity = new Package();

        packageEntity.setDescription(
                "Pacote de teste"
        );
        packageEntity.setRecipient(recipient);
        packageEntity.setUnit(unit);
        packageEntity.setStatus(
                PackageStatus.RECEIVED
        );
        packageEntity.setReceivedAt(
                LocalDateTime.now()
        );

        return packageRepository.save(packageEntity);
    }

    private void authenticate(User user) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        user.getAuthorities()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}