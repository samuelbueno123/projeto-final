package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.Condominium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
}
