package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.CondominiumDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CondominiumDocumentRepository extends JpaRepository<CondominiumDocument, Long> {
    List<CondominiumDocument> findByCondominiumIdOrderByPinnedDescCreatedAtDesc(Long condominiumId);
}
