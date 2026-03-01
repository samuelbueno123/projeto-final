package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
}
