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
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.REJECTED
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

    @Query("""
            select r
            from Reservation r
            join fetch r.reservedBy
            where r.commonArea.id = :commonAreaId
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.CANCELLED
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.REJECTED
              and r.startAt < :endAt
              and r.endAt > :startAt
            order by r.startAt asc
            """)
    List<Reservation> findActiveOverlapping(
            @Param("commonAreaId") Long commonAreaId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    List<Reservation>
    findByCommonAreaCondominiumIdOrderByStartAtDesc(
            Long condominiumId
    );

    @Query("""
        select count(r)
        from Reservation r
        where r.commonArea.condominium.id = :condominiumId
        """)
    long countByCondominiumId(
            Long condominiumId
    );

    @Query("""
        select r.status, count(r)
        from Reservation r
        where r.commonArea.condominium.id = :condominiumId
        group by r.status
        """)
    List<Object[]> countByStatusForCondominium(
            Long condominiumId
    );

    @Query("""
        select count(r)
        from Reservation r
        where r.commonArea.condominium.id = :condominiumId
          and r.status = :status
          and r.startAt > :now
        """)
    long countFutureByCondominiumAndStatus(
            Long condominiumId,
            ReservationStatus status,
            LocalDateTime now
    );
}
