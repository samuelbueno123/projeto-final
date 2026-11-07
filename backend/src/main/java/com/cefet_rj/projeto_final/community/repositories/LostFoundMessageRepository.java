package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.LostFoundMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostFoundMessageRepository extends JpaRepository<LostFoundMessage, Long> {
    List<LostFoundMessage> findByItemIdOrderByCreatedAtAsc(Long itemId);
    long countByItemId(Long itemId);
}
