package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.Vehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    @EntityGraph(attributePaths = {"owner", "unit", "unit.sector", "parkingSpotEntity"})
    List<Vehicle> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"owner", "unit", "unit.sector", "parkingSpotEntity"})
    List<Vehicle> findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(Long condominiumId, Long ownerId);

    @EntityGraph(attributePaths = {"owner", "unit", "unit.sector", "parkingSpotEntity"})
    List<Vehicle> findByCondominiumIdAndLicensePlateContainingIgnoreCaseOrderByCreatedAtDesc(Long condominiumId, String licensePlate);
    boolean existsByCondominiumIdAndLicensePlateIgnoreCase(Long condominiumId, String licensePlate);
    List<Vehicle> findByParkingSpotEntityId(Long parkingSpotId);
}
