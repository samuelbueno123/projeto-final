package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.OccurrenceTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OccurrenceTicketRepository extends JpaRepository<OccurrenceTicket, Long> {
    List<OccurrenceTicket> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<OccurrenceTicket> findByCondominiumIdAndAuthorIdOrderByCreatedAtDesc(Long condominiumId, Long authorId);
}
