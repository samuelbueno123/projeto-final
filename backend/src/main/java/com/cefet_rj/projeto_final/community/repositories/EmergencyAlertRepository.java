package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.EmergencyAlert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    @EntityGraph(attributePaths = {"resident", "unit", "attendedBy"})
    List<EmergencyAlert> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"resident", "unit", "attendedBy"})
    List<EmergencyAlert> findByCondominiumIdAndStatusOrderByCreatedAtDesc(Long condominiumId, String status);
}
