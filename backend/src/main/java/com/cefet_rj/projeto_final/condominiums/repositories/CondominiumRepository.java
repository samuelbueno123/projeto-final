package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
    Page<Condominium> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
