package com.cefet_rj.projeto_final.shared;

import com.cefet_rj.projeto_final.auth.security.CachedUserAuthService;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.BillingCycle;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.reports.controllers.AdminDashboardController;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 6 Query Budget & Caffeine Cache Regression Tests:
 * Ensures zero SQL queries on cache-backed auth/feature hot paths and bounded aggregate query counts on admin overview.
 */
@SpringBootTest(
        properties = {
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "security.client-id=test-client",
                "security.client-secret=test-secret"
        }
)
@ActiveProfiles("test")
class QueryBudgetAndCachingRegressionTests {

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private CachedUserAuthService cachedUserAuthService;
    @Autowired private CondominiumFeatureAccessService featureAccessService;
    @Autowired private AdminDashboardController adminDashboardController;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private CondominiumSubscriptionRepository subscriptionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Hot paths de auth JWT e feature matrix devem servir chamadas repetidas com 0 SQL via Caffeine")
    void caffeineCachePreventsRepeatedDatabaseQueriesForAuthAndFeatureChecks() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cache.guard." + suffix + "@ancora.com";

        Role residentRole = roleRepository.findByAuthority("ROLE_RESIDENT");
        if (residentRole == null) {
            Role r = new Role();
            r.setAuthority("ROLE_RESIDENT");
            residentRole = roleRepository.save(r);
        }

        User user = new User();
        user.setFirstName("Cache");
        user.setLastName("Guard");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("StrongPass#123"));
        user.getRoles().add(residentRole);
        userRepository.save(user);

        Feature packagesFeature = featureRepository.findByCode(FeatureCode.PACKAGES.name())
                .orElseGet(() -> {
                    Feature f = new Feature();
                    f.setCode(FeatureCode.PACKAGES.name());
                    f.setName("Encomendas");
                    return featureRepository.save(f);
                });

        BillingPlan plan = new BillingPlan();
        plan.setName("Plan Cache " + suffix);
        plan.setCode("PLAN_CACHE_" + suffix.toUpperCase());
        plan.setMonthlyFee(new BigDecimal("199.90"));
        plan.setActive(true);
        plan.setFeatures(Set.of(packagesFeature));
        plan = billingPlanRepository.save(plan);

        Condominium condo = new Condominium();
        condo.setName("Condo Cache " + suffix);
        condo.setAddress("Rua Teste, 100");
        condo.setIsActive(true);
        condo.setPlanSelected(true);
        condo.setBillingPlan(plan);
        condo = condominiumRepository.save(condo);

        CondominiumSubscription sub = new CondominiumSubscription();
        sub.setCondominium(condo);
        sub.setBillingPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setStartedAt(LocalDateTime.now().minusDays(2));
        sub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(28));
        subscriptionRepository.save(sub);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        cachedUserAuthService.evictUserAuth(email);
        featureAccessService.evictCondoFeaturesCache(condo.getId());

        // Cold pass warms caches
        stats.clear();
        List<String> roles = cachedUserAuthService.getAuthorityNamesByEmail(email);
        boolean hasPackages = featureAccessService.hasFeature(condo.getId(), FeatureCode.PACKAGES);
        assertTrue(roles.contains("ROLE_RESIDENT"));
        assertTrue(hasPackages);
        long coldStatements = stats.getPrepareStatementCount();
        assertTrue(coldStatements > 0, "Cold lookup should hit DB once");

        // Warm pass (10 iterations) must execute ZERO SQL statements
        stats.clear();
        for (int i = 0; i < 10; i++) {
            assertEquals(roles, cachedUserAuthService.getAuthorityNamesByEmail(email));
            assertTrue(featureAccessService.hasFeature(condo.getId(), FeatureCode.PACKAGES));
        }
        assertEquals(0L, stats.getPrepareStatementCount(),
                "Expected 0 prepared SQL statements during cached warm calls");
    }

    @Test
    @DisplayName("AdminDashboardController overview deve executar orçamento constante de queries agregadas (sem N+1)")
    void adminOverviewUsesBoundedAggregateQueriesRegardlessOfTenantCount() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        BillingPlan plan = billingPlanRepository.findAll().stream().findFirst().orElseGet(() -> {
            BillingPlan bp = new BillingPlan();
            bp.setName("Budget Plan " + suffix);
            bp.setCode("BUDGET_" + suffix.toUpperCase());
            bp.setMonthlyFee(new BigDecimal("299.90"));
            bp.setActive(true);
            return billingPlanRepository.save(bp);
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin.budget@ancora.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
        try {
            Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            stats.setStatisticsEnabled(true);

            // Baseline count before adding extra tenants
            stats.clear();
            adminDashboardController.overview();
            long baselineStatements = stats.getPrepareStatementCount();

            for (int i = 0; i < 6; i++) {
                Condominium c = new Condominium();
                c.setName("Tenant Budget " + suffix + "-" + i);
                c.setAddress("Av. Brasil, " + (100 + i));
                c.setIsActive(i % 2 == 0);
                c.setPlanSelected(true);
                c.setBillingPlan(plan);
                condominiumRepository.save(c);
            }

            stats.clear();
            Map<String, Object> overview = adminDashboardController.overview();
            assertNotNull(overview);
            @SuppressWarnings("unchecked")
            Map<String, Long> volumes = (Map<String, Long>) overview.get("moduleVolumes");
            assertNotNull(volumes);
            assertTrue(volumes.get("condominiums") >= 6);
            assertTrue(((Number) overview.get("activeCondominiums")).longValue() >= 3);

            long afterTenantsStatements = stats.getPrepareStatementCount();
            assertEquals(baselineStatements, afterTenantsStatements,
                    "Admin overview query count must remain strictly O(1) when scaling tenant count");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
