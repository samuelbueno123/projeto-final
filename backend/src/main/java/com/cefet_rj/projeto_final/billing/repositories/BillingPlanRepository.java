package com.cefet_rj.projeto_final.billing.repositories;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, Long> {

    Optional<BillingPlan> findByNameIgnoreCase(String name);
}
