package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.SmartLockerCompartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmartLockerCompartmentRepository extends JpaRepository<SmartLockerCompartment, Long> {
    List<SmartLockerCompartment> findByCondominiumIdOrderByDepositedAtDesc(Long condominiumId);
    List<SmartLockerCompartment> findByCondominiumIdAndRecipientIdOrderByDepositedAtDesc(Long condominiumId, Long recipientId);
}
