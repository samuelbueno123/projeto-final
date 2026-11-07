package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.VisitorAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorAccessRepository extends JpaRepository<VisitorAccess, Long> {
    List<VisitorAccess> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<VisitorAccess> findByCondominiumIdAndResidentIdOrderByCreatedAtDesc(Long condominiumId, Long residentId);
    List<VisitorAccess> findByCondominiumIdAndVehiclePlateContainingIgnoreCaseOrderByCreatedAtDesc(Long condominiumId, String vehiclePlate);
    Optional<VisitorAccess> findByCondominiumIdAndAccessCodeIgnoreCase(Long condominiumId, String accessCode);
    Optional<VisitorAccess> findByCondominiumIdAndQrCodePayload(Long condominiumId, String qrCodePayload);
}
