package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    @EntityGraph(attributePaths = {"condominium"})
    @NonNull
    Page<Sector> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"condominium"})
    Page<Sector> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"condominium"})
    List<Sector> findByCondominiumId(Long condominiumId);

    Optional<Sector> findByCondominiumIdAndNameIgnoreCase(Long condominiumId, String name);
}
