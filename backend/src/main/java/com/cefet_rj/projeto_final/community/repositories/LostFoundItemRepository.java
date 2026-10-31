package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.LostFoundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostFoundItemRepository extends JpaRepository<LostFoundItem, Long> {
    List<LostFoundItem> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
}
