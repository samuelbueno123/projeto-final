package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<Vehicle> findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(Long condominiumId, Long ownerId);
    List<Vehicle> findByCondominiumIdAndLicensePlateContainingIgnoreCaseOrderByCreatedAtDesc(Long condominiumId, String licensePlate);
}
