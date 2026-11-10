package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.RenovationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RenovationRequestRepository extends JpaRepository<RenovationRequest, Long> {
    List<RenovationRequest> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<RenovationRequest> findByCondominiumIdAndRequesterIdOrderByCreatedAtDesc(Long condominiumId, Long requesterId);
    List<RenovationRequest> findByCondominiumIdAndStatusOrderByCreatedAtDesc(Long condominiumId, String status);
}
