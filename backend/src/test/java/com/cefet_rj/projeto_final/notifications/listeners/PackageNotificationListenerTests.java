package com.cefet_rj.projeto_final.notifications.listeners;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.events.PackageStatusChangedEvent;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class PackageNotificationListenerTests {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private UserRepository userRepository;

    private Condominium basicCondominium;
    private Condominium premiumCondominium;

    private User basicResident;
    private User premiumResident;

    @BeforeEach
    void setUp() {

        notificationRepository.deleteAll();
        condominiumRepository.deleteAll();
        billingPlanRepository.deleteAll();
        featureRepository.deleteAll();
        userRepository.deleteAll();

        Feature packagesFeature = createFeature(
                FeatureCode.PACKAGES,
                "Encomendas"
        );

        Feature notificationsFeature = createFeature(
                FeatureCode.NOTIFICATIONS,
                "Notificações"
        );

        BillingPlan basicPlan = createPlan(
                "BASIC",
                new BigDecimal("99.90"),
                Set.of(packagesFeature)
        );

        BillingPlan premiumPlan = createPlan(
                "PREMIUM",
                new BigDecimal("199.90"),
                Set.of(
                        packagesFeature,
                        notificationsFeature
                )
        );

        basicCondominium = createCondominium(
                "Condomínio Basic",
                basicPlan
        );

        premiumCondominium = createCondominium(
                "Condomínio Premium",
                premiumPlan
        );

        basicResident = createUser(
                "Morador Basic",
                "basic@test.com"
        );

        premiumResident = createUser(
                "Morador Premium",
                "premium@test.com"
        );
    }

    @Test
    void basicPlanShouldNotCreateNotification() {

        PackageStatusChangedEvent event =
                new PackageStatusChangedEvent(
                        100L,
                        basicResident.getId(),
                        basicCondominium.getId(),
                        PackageStatus.RECEIVED,
                        PackageStatus.NOTIFIED,
                        LocalDateTime.now()
                );

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(event)
        );

        assertEquals(
                0,
                notificationRepository.count()
        );
    }

    @Test
    void premiumPlanShouldCreateNotification() {

        PackageStatusChangedEvent event =
                new PackageStatusChangedEvent(
                        200L,
                        premiumResident.getId(),
                        premiumCondominium.getId(),
                        PackageStatus.RECEIVED,
                        PackageStatus.NOTIFIED,
                        LocalDateTime.now()
                );

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(event)
        );

        assertEquals(
                1,
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
        plan.setFeatures(new HashSet<>(features));

        return billingPlanRepository.save(plan);
    }

    private Condominium createCondominium(
            String name,
            BillingPlan billingPlan
    ) {
        Condominium condominium = new Condominium();

        condominium.setName(name);
        condominium.setAddress(
                "Endereço de teste - " + name
        );
        condominium.setBillingPlan(billingPlan);
        condominium.setIsActive(true);

        return condominiumRepository.save(condominium);
    }

    private User createUser(
            String firstName,
            String email
    ) {
        User user = new User();

        user.setFirstName(firstName);
        user.setLastName("Teste");
        user.setEmail(email);
        user.setPassword("123456");

        return userRepository.save(user);
    }
}