package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.MovingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MovingRequestRepository extends JpaRepository<MovingRequest, Long> {
    List<MovingRequest> findByCondominiumIdOrderByMovingDateDescCreatedAtDesc(Long condominiumId);
    List<MovingRequest> findByCondominiumIdAndRequestedByIdOrderByMovingDateDescCreatedAtDesc(Long condominiumId, Long userId);
    List<MovingRequest> findByCondominiumIdAndMovingDateOrderByCreatedAtAsc(Long condominiumId, LocalDate movingDate);
}
