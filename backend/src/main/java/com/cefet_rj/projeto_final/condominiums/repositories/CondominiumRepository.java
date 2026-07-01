package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
}
