package com.cefet_rj.projeto_final.maintenance.repositories;

import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {

    @Query("SELECT m FROM MaintenanceOrder m " +
           "LEFT JOIN FETCH m.condominium c " +
           "LEFT JOIN FETCH m.createdBy u " +
           "WHERE m.condominium.id = :condominiumId " +
           "ORDER BY m.createdAt DESC")
    List<MaintenanceOrder> findAllByCondominiumIdWithDetails(@Param("condominiumId") Long condominiumId);

    @Query("SELECT m FROM MaintenanceOrder m " +
           "LEFT JOIN FETCH m.condominium c " +
           "LEFT JOIN FETCH m.createdBy u " +
           "WHERE m.condominium.id = :condominiumId AND m.status = :status " +
           "ORDER BY m.createdAt DESC")
    List<MaintenanceOrder> findAllByCondominiumIdAndStatusWithDetails(@Param("condominiumId") Long condominiumId, @Param("status") String status);

    @Query("SELECT m FROM MaintenanceOrder m " +
           "LEFT JOIN FETCH m.condominium c " +
           "LEFT JOIN FETCH m.createdBy u " +
           "WHERE m.id = :id")
    Optional<MaintenanceOrder> findByIdWithDetails(@Param("id") Long id);

    long countByCondominiumIdAndStatus(Long condominiumId, String status);
}
