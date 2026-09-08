package com.cefet_rj.projeto_final.announcements.repositories;

import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    List<Announcement> findByCondominiumIdAndActiveTrueOrderByCreatedAtDesc(
            Long condominiumId
    );

    List<Announcement> findByCondominiumIdOrderByCreatedAtDesc(
            Long condominiumId
    );
}