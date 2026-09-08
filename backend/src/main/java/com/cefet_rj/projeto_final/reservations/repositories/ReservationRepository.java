package com.cefet_rj.projeto_final.reservations.repositories;

import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation>
    findByReservedByIdAndCommonAreaCondominiumIdOrderByStartAtDesc(
            Long userId,
            Long condominiumId
    );

    @Query("""
            select count(r)
            from Reservation r
            where r.commonArea.id = :commonAreaId
              and r.status <> :cancelledStatus
              and r.startAt < :endAt
              and r.endAt > :startAt
            """)
    long countConflictingReservations(
            @Param("commonAreaId")
            Long commonAreaId,

            @Param("startAt")
            LocalDateTime startAt,

            @Param("endAt")
            LocalDateTime endAt,

            @Param("cancelledStatus")
            ReservationStatus cancelledStatus
    );
}