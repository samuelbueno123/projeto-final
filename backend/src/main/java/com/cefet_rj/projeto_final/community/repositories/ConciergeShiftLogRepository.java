package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.ConciergeShiftLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConciergeShiftLogRepository extends JpaRepository<ConciergeShiftLog, Long> {
    List<ConciergeShiftLog> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
}
