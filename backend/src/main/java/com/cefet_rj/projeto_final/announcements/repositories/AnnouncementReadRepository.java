package com.cefet_rj.projeto_final.announcements.repositories;

import com.cefet_rj.projeto_final.announcements.entities.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    @Query("SELECT r FROM AnnouncementRead r JOIN FETCH r.user WHERE r.announcement.id = :announcementId ORDER BY r.readAt DESC")
    List<AnnouncementRead> findByAnnouncementIdOrderByReadAtDesc(@Param("announcementId") Long announcementId);

    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    long countByAnnouncementId(Long announcementId);

    @Query("SELECT r FROM AnnouncementRead r WHERE r.announcement.id IN :announcementIds")
    List<AnnouncementRead> findAllByAnnouncementIds(@Param("announcementIds") List<Long> announcementIds);
}
