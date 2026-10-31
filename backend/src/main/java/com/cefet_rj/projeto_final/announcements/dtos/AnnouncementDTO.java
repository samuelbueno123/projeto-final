package com.cefet_rj.projeto_final.announcements.dtos;

import com.cefet_rj.projeto_final.announcements.entities.Announcement;

import java.time.LocalDateTime;

public record AnnouncementDTO(

        Long id,
        String title,
        String content,
        String imageUrl,
        Long condominiumId,
        String condominiumName,
        Long createdById,
        String createdByName,
        String createdByEmail,
        Long unitId,
        String unitName,
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
                entity.getCondominium().getName(),
                entity.getCreatedBy().getId(),
                (entity.getCreatedBy().getFirstName() + " "
                        + entity.getCreatedBy().getLastName()).trim(),
                entity.getCreatedBy().getEmail(),
                entity.getUnit() != null ? entity.getUnit().getId() : null,
                entity.getUnit() != null
                        ? entity.getUnit().getSector().getName()
                        + " • Unidade " + entity.getUnit().getIdentifier()
                        : null,
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getActive()
        );
    }
}
