package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    Page<Sector> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Sector> findByCondominiumId(Long condominiumId);
    Optional<Sector> findByCondominiumIdAndNameIgnoreCase(Long condominiumId, String name);
}
