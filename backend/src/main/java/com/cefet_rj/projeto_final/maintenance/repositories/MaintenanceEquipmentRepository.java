package com.cefet_rj.projeto_final.maintenance.repositories;

import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceEquipmentRepository extends JpaRepository<MaintenanceEquipment, Long> {
    List<MaintenanceEquipment> findByCondominiumIdOrderByNextMaintenanceDateAsc(Long condominiumId);
}
