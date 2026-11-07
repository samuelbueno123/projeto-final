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

    @Query("""
            select r
            from Reservation r
            join fetch r.commonArea ca
            join fetch r.reservedBy u
            where u.id = :userId and ca.condominium.id = :condominiumId
            order by r.startAt desc
            """)
    List<Reservation>
    findByReservedByIdAndCommonAreaCondominiumIdOrderByStartAtDesc(
            @Param("userId") Long userId,
            @Param("condominiumId") Long condominiumId
    );

    @Query("""
            select count(r)
            from Reservation r
            where r.commonArea.id = :commonAreaId
              and r.status <> :cancelledStatus
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.REJECTED
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.WAITLISTED
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
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.WAITLISTED
              and r.startAt < :endAt
              and r.endAt > :startAt
            order by r.startAt asc
            """)
    List<Reservation> findActiveOverlapping(
            @Param("commonAreaId") Long commonAreaId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            select r
            from Reservation r
            join fetch r.commonArea ca
            join fetch r.reservedBy
            where r.commonArea.id = :commonAreaId
              and r.status = com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.WAITLISTED
              and r.startAt < :endAt
              and r.endAt > :startAt
            order by coalesce(r.waitlistPosition, 999999) asc, r.createdAt asc
            """)
    List<Reservation> findWaitlistedOverlapping(
            @Param("commonAreaId") Long commonAreaId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            select r
            from Reservation r
            join fetch r.commonArea ca
            join fetch r.reservedBy
            where r.commonArea.id = :commonAreaId
              and r.status = com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.WAITLISTED
            order by coalesce(r.waitlistPosition, 999999) asc, r.createdAt asc
            """)
    List<Reservation> findWaitlistedByCommonArea(
            @Param("commonAreaId") Long commonAreaId
    );

    @Query("""
            select count(r)
            from Reservation r
            where r.commonArea.id = :commonAreaId
              and r.reservedBy.id = :userId
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.CANCELLED
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.REJECTED
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.WAITLISTED
              and r.startAt >= :fromDate
              and r.startAt < :toDate
            """)
    long countActiveBookingsByUserInRange(
            @Param("commonAreaId") Long commonAreaId,
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
            select count(r)
            from Reservation r
            where r.commonArea.id = :commonAreaId
              and r.reservedBy.id = :userId
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.CANCELLED
              and r.status <> com.cefet_rj.projeto_final.reservations.enums.ReservationStatus.REJECTED
              and r.startAt < :endAt
              and r.endAt > :startAt
            """)
    long countUserOverlappingReservationsOrWaitlist(
            @Param("commonAreaId") Long commonAreaId,
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            select r
            from Reservation r
            join fetch r.commonArea ca
            join fetch r.reservedBy u
            where ca.condominium.id = :condominiumId
            order by r.startAt desc
            """)
    List<Reservation>
    findByCommonAreaCondominiumIdOrderByStartAtDesc(
            @Param("condominiumId") Long condominiumId
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
