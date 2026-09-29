package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {
}
