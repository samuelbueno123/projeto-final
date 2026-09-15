package com.cefet_rj.projeto_final.billing.repositories;

import com.cefet_rj.projeto_final.billing.entities.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {

    Optional<Feature> findByCode(String code);
}