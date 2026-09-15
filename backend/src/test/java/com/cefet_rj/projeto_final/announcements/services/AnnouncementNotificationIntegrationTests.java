package com.cefet_rj.projeto_final.announcements.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
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
import org.springframework.transaction.support.TransactionTemplate;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AnnouncementNotificationIntegrationTests {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AnnouncementRepository announcementRepository;

    private User admin;
    private User resident;

    @BeforeEach
    void setup() {

        notificationRepository.deleteAll();
        announcementRepository.deleteAll();
        membershipRepository.deleteAll();

        condominiumRepository.deleteAll();
        billingPlanRepository.deleteAll();
        featureRepository.deleteAll();

        userRepository.deleteAll();
        roleRepository.deleteAll();

        notificationRepository.deleteAll();
        membershipRepository.deleteAll();

        Role adminRole = roleRepository.findByAuthority("ROLE_ADMIN");

        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setAuthority("ROLE_ADMIN");
            adminRole = roleRepository.save(adminRole);
        }

        Role residentRole = roleRepository.findByAuthority("ROLE_RESIDENT");

        if (residentRole == null) {
            residentRole = new Role();
            residentRole.setAuthority("ROLE_RESIDENT");
            residentRole = roleRepository.save(residentRole);
        }

        admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Announcement");
        admin.setEmail("admin-announcement-flow@test.com");
        admin.setPassword("123456");
        admin.addRole(adminRole);
        admin = userRepository.save(admin);

        resident = new User();
        resident.setFirstName("Resident");
        resident.setLastName("Announcement");
        resident.setEmail("resident-announcement-flow@test.com");
        resident.setPassword("123456");
        resident.addRole(residentRole);
        resident = userRepository.save(resident);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getEmail(),
                        null,
                        admin.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();

        notificationRepository.deleteAll();
        announcementRepository.deleteAll();
        membershipRepository.deleteAll();

        condominiumRepository.deleteAll();
        billingPlanRepository.deleteAll();
        featureRepository.deleteAll();

        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void shouldCreateNotificationForResidentAfterAnnouncementCommit() {

        Feature announcementsFeature =
                getOrCreateFeature(FeatureCode.ANNOUNCEMENTS);

        Feature notificationsFeature =
                getOrCreateFeature(FeatureCode.NOTIFICATIONS);

        BillingPlan plan = new BillingPlan();
        plan.setName("ANNOUNCEMENT_NOTIFICATION_PLAN");
        plan.setMonthlyFee(new BigDecimal("199.90"));
        plan.setActive(true);
        plan.setFeatures(
                Set.of(
                        announcementsFeature,
                        notificationsFeature
                )
        );

        plan = billingPlanRepository.save(plan);

        Condominium condominium = new Condominium();
        condominium.setName("Notification Condominium");
        condominium.setAddress("Test Address");
        condominium.setBillingPlan(plan);
        condominium.setIsActive(true);

        Condominium savedCondominium =
                condominiumRepository.save(condominium);

        Membership membership = new Membership();
        membership.setUser(resident);
        membership.setCondominium(savedCondominium);
        membership.setRole(
                roleRepository.findByAuthority("ROLE_RESIDENT")
        );

        membershipRepository.save(membership);

        AnnouncementRequestDTO request =
                new AnnouncementRequestDTO(
                        "Elevator maintenance",
                        "Elevator will be unavailable tomorrow.",
                        null
                );

        transactionTemplate.executeWithoutResult(status ->
                announcementService.create(
                        savedCondominium.getId(),
                        request
                )
        );

        List<Notification> notifications =
                notificationRepository.findAll();

        assertEquals(1, notifications.size());

        Notification notification =
                notifications.getFirst();

        assertEquals(
                NotificationType.ANNOUNCEMENT,
                notification.getType()
        );

        assertEquals(
                resident.getId(),
                notification.getRecipient().getId()
        );

        assertFalse(notification.getRead());

        assertTrue(
                notification.getMessage()
                        .contains("Elevator maintenance")
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