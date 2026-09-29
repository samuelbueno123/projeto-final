package com.cefet_rj.projeto_final.reservations.repositories;

import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommonAreaRepository
        extends JpaRepository<CommonArea, Long> {

    List<CommonArea>
    findByCondominiumIdAndActiveTrueOrderByNameAsc(
            Long condominiumId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ca
            from CommonArea ca
            where ca.id = :id
            """)
    Optional<CommonArea> findByIdForUpdate(
            @Param("id") Long id
    );
}