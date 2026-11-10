package com.cefet_rj.projeto_final.shared.observability.repositories;

import com.cefet_rj.projeto_final.shared.observability.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    long countByCategory(String category);

    long countBySeverity(String severity);

    long countByOutcome(String outcome);

    List<AuditLog> findTop15ByOrderByTimestampDesc();

    @Query("SELECT a.category, COUNT(a) FROM AuditLog a GROUP BY a.category")
    List<Object[]> countGroupedByCategory();

    @Query("SELECT a.severity, COUNT(a) FROM AuditLog a GROUP BY a.severity")
    List<Object[]> countGroupedBySeverity();

    @Query("SELECT a.outcome, COUNT(a) FROM AuditLog a GROUP BY a.outcome")
    List<Object[]> countGroupedByOutcome();
}
