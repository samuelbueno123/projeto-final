package com.cefet_rj.projeto_final.announcements.repositories;

import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    @Query("""
            select a
            from Announcement a
            join fetch a.condominium
            join fetch a.createdBy
            left join fetch a.sector
            left join fetch a.unit u
            left join fetch u.sector
            where a.condominium.id = :condominiumId
              and a.active = true
            order by a.createdAt desc
            """)
    List<Announcement> findByCondominiumIdAndActiveTrueOrderByCreatedAtDesc(
            @Param("condominiumId") Long condominiumId
    );

    @Query("""
            select a
            from Announcement a
            join fetch a.condominium
            join fetch a.createdBy
            left join fetch a.sector
            left join fetch a.unit u
            left join fetch u.sector
            where a.condominium.id = :condominiumId
            order by a.createdAt desc
            """)
    List<Announcement> findByCondominiumIdOrderByCreatedAtDesc(
            @Param("condominiumId") Long condominiumId
    );

    @Query("""
        select count(a)
        from Announcement a
        where a.condominium.id = :condominiumId
          and a.active = true
          and (
                a.expiresAt is null
                or a.expiresAt > :now
              )
        """)
    long countActiveByCondominium(
            @Param("condominiumId") Long condominiumId,
            @Param("now") LocalDateTime now
    );
}
