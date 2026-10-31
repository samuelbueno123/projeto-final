package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.ClassifiedAd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassifiedAdRepository extends JpaRepository<ClassifiedAd, Long> {
    List<ClassifiedAd> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
}
