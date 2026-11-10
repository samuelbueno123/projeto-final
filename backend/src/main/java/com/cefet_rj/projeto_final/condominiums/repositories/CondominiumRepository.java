package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
    @EntityGraph(attributePaths = {"billingPlan"})
    @NonNull
    Page<Condominium> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"billingPlan"})
    Page<Condominium> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
