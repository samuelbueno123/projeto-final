package com.cefet_rj.projeto_final.manager.repositories;

import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UnitPaymentRepository extends JpaRepository<UnitPayment, Long> {
    Optional<UnitPayment> findByUnitId(Long unitId);
}
