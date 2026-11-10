package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.SmartLockerCompartment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmartLockerCompartmentRepository extends JpaRepository<SmartLockerCompartment, Long> {
    @EntityGraph(attributePaths = {"recipient", "unit", "unit.sector", "depositedBy"})
    List<SmartLockerCompartment> findByCondominiumIdOrderByDepositedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"recipient", "unit", "unit.sector", "depositedBy"})
    List<SmartLockerCompartment> findByCondominiumIdAndRecipientIdOrderByDepositedAtDesc(Long condominiumId, Long recipientId);
}
