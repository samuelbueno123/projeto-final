package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.RenovationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RenovationRequestRepository extends JpaRepository<RenovationRequest, Long> {
    @EntityGraph(attributePaths = {"requester", "unit", "decidedBy"})
    List<RenovationRequest> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"requester", "unit", "decidedBy"})
    List<RenovationRequest> findByCondominiumIdAndRequesterIdOrderByCreatedAtDesc(Long condominiumId, Long requesterId);

    @EntityGraph(attributePaths = {"requester", "unit", "decidedBy"})
    List<RenovationRequest> findByCondominiumIdAndStatusOrderByCreatedAtDesc(Long condominiumId, String status);
}
