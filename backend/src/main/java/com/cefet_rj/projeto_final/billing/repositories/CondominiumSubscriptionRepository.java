package com.cefet_rj.projeto_final.billing.repositories;

import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CondominiumSubscriptionRepository extends JpaRepository<CondominiumSubscription, Long> {

    @EntityGraph(attributePaths = {"condominium", "billingPlan", "billingPlan.features"})
    Optional<CondominiumSubscription> findByCondominiumId(Long condominiumId);

    @Query("""
            select s
            from CondominiumSubscription s
            join fetch s.condominium c
            join fetch s.billingPlan p
            left join fetch p.features
            order by c.name asc
            """)
    List<CondominiumSubscription> findAllWithDetails();
}
