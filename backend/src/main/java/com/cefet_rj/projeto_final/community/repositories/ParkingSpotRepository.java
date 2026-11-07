package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByCondominiumIdOrderByCodeAsc(Long condominiumId);
    List<ParkingSpot> findByCondominiumIdAndUnitIdOrderByCodeAsc(Long condominiumId, Long unitId);
    boolean existsByCondominiumIdAndCodeIgnoreCase(Long condominiumId, String code);
}
