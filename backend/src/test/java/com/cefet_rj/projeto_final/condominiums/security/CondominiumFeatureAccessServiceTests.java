package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CondominiumFeatureAccessServiceTests {

    @Autowired
    private CondominiumFeatureAccessService featureAccessService;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private FeatureRepository featureRepository;

    private Condominium basicCondominium;
    private Condominium premiumCondominium;

    private Feature packagesFeature;
    private Feature notificationsFeature;
    private Feature reportsFeature;

    @BeforeEach
    void setUp() {

        condominiumRepository.deleteAll();
        billingPlanRepository.deleteAll();
        featureRepository.deleteAll();

        packagesFeature = createFeature(
                FeatureCode.PACKAGES,
                "Encomendas"
        );

        notificationsFeature = createFeature(
                FeatureCode.NOTIFICATIONS,
                "Notificações"
        );

        reportsFeature = createFeature(
                FeatureCode.REPORTS,
                "Relatórios"
        );

        BillingPlan basic = createPlan(
                "BASIC",
                new BigDecimal("99.90"),
                Set.of(packagesFeature)
        );

        BillingPlan premium = createPlan(
                "PREMIUM",
                new BigDecimal("199.90"),
                Set.of(
                        packagesFeature,
                        notificationsFeature,
                        reportsFeature
                )
        );

        basicCondominium = createCondominium(
                "Condomínio Basic",
                basic
        );

        premiumCondominium = createCondominium(
                "Condomínio Premium",
                premium
        );
    }

    @Test
    void basicShouldHavePackagesFeature() {

        boolean enabled = featureAccessService.hasFeature(
                basicCondominium.getId(),
                FeatureCode.PACKAGES
        );

        assertTrue(enabled);
    }

    @Test
    void basicShouldNotHaveNotificationsFeature() {

        boolean enabled = featureAccessService.hasFeature(
                basicCondominium.getId(),
                FeatureCode.NOTIFICATIONS
        );

        assertFalse(enabled);
    }

    @Test
    void premiumShouldHavePackagesFeature() {

        boolean enabled = featureAccessService.hasFeature(
                premiumCondominium.getId(),
                FeatureCode.PACKAGES
        );

        assertTrue(enabled);
    }

    @Test
    void premiumShouldHaveNotificationsFeature() {

        boolean enabled = featureAccessService.hasFeature(
                premiumCondominium.getId(),
                FeatureCode.NOTIFICATIONS
        );

        assertTrue(enabled);
    }

    @Test
    void shouldCreateNewPlanWithoutChangingFeatureAccessCode() {

        BillingPlan standard = createPlan(
                "STANDARD",
                new BigDecimal("149.90"),
                Set.of(
                        packagesFeature,
                        notificationsFeature,
                        reportsFeature
                )
        );

        Condominium standardCondominium = createCondominium(
                "Condomínio Standard",
                standard
        );

        assertTrue(
                featureAccessService.hasFeature(
                        standardCondominium.getId(),
                        FeatureCode.PACKAGES
                )
        );

        assertTrue(
                featureAccessService.hasFeature(
                        standardCondominium.getId(),
                        FeatureCode.NOTIFICATIONS
                )
        );

        assertTrue(
                featureAccessService.hasFeature(
                        standardCondominium.getId(),
                        FeatureCode.REPORTS
                )
        );

        assertFalse(
                featureAccessService.hasFeature(
                        standardCondominium.getId(),
                        FeatureCode.RESERVATIONS
                )
        );
    }

    @Test
    void shouldAllowEnabledFeature() {

        assertDoesNotThrow(() ->
                featureAccessService.validateFeatureEnabled(
                        basicCondominium.getId(),
                        FeatureCode.PACKAGES
                )
        );
    }

    @Test
    void shouldRejectDisabledFeature() {

        assertThrows(
                ForbiddenException.class,
                () ->
                        featureAccessService.validateFeatureEnabled(
                                basicCondominium.getId(),
                                FeatureCode.NOTIFICATIONS
                        )
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
}