package com.cefet_rj.projeto_final.announcements.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnnouncementServiceIntegrationTests {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User admin;

    @BeforeEach
    void setup() {

        Role adminRole = roleRepository.findByAuthority("ROLE_ADMIN");

        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setAuthority("ROLE_ADMIN");
            adminRole = roleRepository.save(adminRole);
        }

        admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@announcement.test");
        admin.setPassword("123456");
        admin.addRole(adminRole);
        admin = userRepository.save(admin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getEmail(),
                        null,
                        admin.getAuthorities()
                )
        );
    }

    @Test
    void shouldCreateAnnouncementWhenFeatureIsEnabled() {

        Feature announcementsFeature =
                getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        BillingPlan plan = new BillingPlan();
        plan.setName("ANNOUNCEMENT_ENABLED");
        plan.setMonthlyFee(new BigDecimal("150.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of(announcementsFeature));
        plan = billingPlanRepository.save(plan);

        Condominium condominium = new Condominium();
        condominium.setName("Condominium With Announcements");
        condominium.setAddress("Test Address");
        condominium.setBillingPlan(plan);
        condominium.setIsActive(true);
        condominium = condominiumRepository.save(condominium);

        AnnouncementRequestDTO request =
                new AnnouncementRequestDTO(
                        "Water maintenance",
                        "Water will be unavailable tomorrow.",
                        null
                );

        var result =
                announcementService.create(
                        condominium.getId(),
                        request
                );

        assertNotNull(result.id());
        assertEquals("Water maintenance", result.title());
        assertEquals(condominium.getId(), result.condominiumId());
        assertTrue(result.active());
    }

    @Test
    void shouldRejectAnnouncementWhenFeatureIsDisabled() {

        BillingPlan plan = new BillingPlan();
        plan.setName("ANNOUNCEMENT_DISABLED");
        plan.setMonthlyFee(new BigDecimal("99.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of());
        plan = billingPlanRepository.save(plan);

        Condominium condominium = new Condominium();
        condominium.setName("Condominium Without Announcements");
        condominium.setAddress("Test Address");
        condominium.setBillingPlan(plan);
        condominium.setIsActive(true);

        Condominium savedCondominium =
                condominiumRepository.save(condominium);

        AnnouncementRequestDTO request =
                new AnnouncementRequestDTO(
                        "Test",
                        "This should not be created.",
                        null
                );

        assertThrows(
                ForbiddenException.class,
                () -> announcementService.create(
                        savedCondominium.getId(),
                        request
                )
        );
    }

    private Feature getOrCreateFeature(
            FeatureCode featureCode
    ) {

        return featureRepository
                .findByCode(featureCode.name())
                .orElseGet(() -> {

                    Feature feature = new Feature();
                    feature.setCode(featureCode.name());
                    feature.setName(featureCode.name());

                    return featureRepository.save(feature);
                });
    }
}