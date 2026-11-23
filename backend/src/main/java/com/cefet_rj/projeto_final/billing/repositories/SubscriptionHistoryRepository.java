package com.cefet_rj.projeto_final.billing.repositories;

import com.cefet_rj.projeto_final.billing.entities.SubscriptionHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    @EntityGraph(attributePaths = {"previousPlan", "newPlan", "changedBy"})
    List<SubscriptionHistory> findTop50ByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
}
