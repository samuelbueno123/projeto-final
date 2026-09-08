package com.cefet_rj.projeto_final.announcements.events;

import java.time.LocalDateTime;

public record AnnouncementCreatedEvent(
        Long announcementId,
        Long condominiumId,
        String title,
        LocalDateTime occurredAt
) {
}