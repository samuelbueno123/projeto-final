package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
}
