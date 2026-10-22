package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    Page<Sector> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
