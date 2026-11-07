package com.cefet_rj.projeto_final.announcements.dtos;

import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import com.cefet_rj.projeto_final.announcements.entities.AnnouncementRead;

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
        Long sectorId,
        String sectorName,
        Long unitId,
        String unitName,
        String targetRole,
        long readCount,
        boolean readByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Boolean active
) {

    public AnnouncementDTO(Announcement entity) {
        this(entity, 0L, false);
    }

    public AnnouncementDTO(Announcement entity, long readCount, boolean readByCurrentUser) {
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
                entity.getSector() != null ? entity.getSector().getId() : null,
                entity.getSector() != null ? entity.getSector().getName() : null,
                entity.getUnit() != null ? entity.getUnit().getId() : null,
                entity.getUnit() != null
                        ? entity.getUnit().getSector().getName()
                        + " • Unidade " + entity.getUnit().getIdentifier()
                        : null,
                entity.getTargetRole(),
                readCount,
                readByCurrentUser,
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getActive()
        );
    }

    public record AnnouncementReadDTO(
            Long id,
            Long announcementId,
            Long userId,
            String userName,
            String userEmail,
            LocalDateTime readAt
    ) {
        public AnnouncementReadDTO(AnnouncementRead r) {
            this(
                    r.getId(),
                    r.getAnnouncement().getId(),
                    r.getUser().getId(),
                    (r.getUser().getFirstName() + " " + r.getUser().getLastName()).trim(),
                    r.getUser().getEmail(),
                    r.getReadAt()
            );
        }
    }
}
