package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.VisitorAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorAccessRepository extends JpaRepository<VisitorAccess, Long> {
    @EntityGraph(attributePaths = {"resident", "unit", "unit.sector"})
    List<VisitorAccess> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"resident", "unit", "unit.sector"})
    List<VisitorAccess> findByCondominiumIdAndResidentIdOrderByCreatedAtDesc(Long condominiumId, Long residentId);

    @EntityGraph(attributePaths = {"resident", "unit", "unit.sector"})
    List<VisitorAccess> findByCondominiumIdAndVehiclePlateContainingIgnoreCaseOrderByCreatedAtDesc(Long condominiumId, String vehiclePlate);
    Optional<VisitorAccess> findByCondominiumIdAndAccessCodeIgnoreCase(Long condominiumId, String accessCode);
    Optional<VisitorAccess> findByCondominiumIdAndQrCodePayload(Long condominiumId, String qrCodePayload);
}
