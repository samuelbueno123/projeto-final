package com.cefet_rj.projeto_final.financial.repositories;

import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CondominiumExpenseRepository extends JpaRepository<CondominiumExpense, Long> {

    @Query("""
        SELECT e FROM CondominiumExpense e
        JOIN FETCH e.condominium c
        LEFT JOIN FETCH e.createdBy u
        WHERE c.id = :condoId
        ORDER BY e.dueDate DESC
    """)
    List<CondominiumExpense> findByCondominiumIdOrderByDueDateDesc(@Param("condoId") Long condoId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM CondominiumExpense e
        WHERE e.condominium.id = :condoId
          AND e.status = :status
    """)
    BigDecimal sumAmountByCondominiumIdAndStatus(
            @Param("condoId") Long condoId,
            @Param("status") String status
    );
}
