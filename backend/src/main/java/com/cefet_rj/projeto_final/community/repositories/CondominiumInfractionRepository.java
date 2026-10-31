package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.CondominiumInfraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CondominiumInfractionRepository extends JpaRepository<CondominiumInfraction, Long> {
    List<CondominiumInfraction> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<CondominiumInfraction> findByCondominiumIdAndUnitIdOrderByCreatedAtDesc(Long condominiumId, Long unitId);
}
