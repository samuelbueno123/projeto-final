package com.cefet_rj.projeto_final.announcements.events;

import java.time.LocalDateTime;

public record AnnouncementCreatedEvent(
        Long announcementId,
        Long condominiumId,
        String condominiumName,
        Long unitId,
        Long sectorId,
        String targetRole,
        String title,
        LocalDateTime occurredAt
) {
    public AnnouncementCreatedEvent(
            Long announcementId,
            Long condominiumId,
            String condominiumName,
            Long unitId,
            String title,
            LocalDateTime occurredAt
    ) {
        this(announcementId, condominiumId, condominiumName, unitId, null, null, title, occurredAt);
    }
}
