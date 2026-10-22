package com.cefet_rj.projeto_final.announcements.dtos;

import com.cefet_rj.projeto_final.announcements.entities.Announcement;

import java.time.LocalDateTime;

public record AnnouncementDTO(

        Long id,
        String title,
        String content,
        String imageUrl,
        Long condominiumId,
        Long createdById,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Boolean active

) {

    public AnnouncementDTO(Announcement entity) {
        this(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getImageUrl(),
                entity.getCondominium().getId(),
                entity.getCreatedBy().getId(),
                entity.getCreatedBy().getEmail(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getActive()
        );
    }
}
