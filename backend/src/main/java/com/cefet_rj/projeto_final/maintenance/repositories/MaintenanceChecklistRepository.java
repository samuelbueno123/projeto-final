package com.cefet_rj.projeto_final.maintenance.repositories;

import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceChecklistRepository extends JpaRepository<MaintenanceChecklist, Long> {
    List<MaintenanceChecklist> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<MaintenanceChecklist> findByCondominiumIdAndStatusOrderByCreatedAtDesc(Long condominiumId, String status);
}
