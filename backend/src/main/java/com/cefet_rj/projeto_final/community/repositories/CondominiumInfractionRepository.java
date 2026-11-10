package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.CondominiumInfraction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CondominiumInfractionRepository extends JpaRepository<CondominiumInfraction, Long> {
    @EntityGraph(attributePaths = {"unit", "unit.sector", "issuedBy"})
    List<CondominiumInfraction> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"unit", "unit.sector", "issuedBy"})
    List<CondominiumInfraction> findByCondominiumIdAndUnitIdOrderByCreatedAtDesc(Long condominiumId, Long unitId);
}
