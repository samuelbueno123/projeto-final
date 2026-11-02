package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.CondominiumGalleryImage;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CondominiumGalleryDTO(
        Long id,
        Long condominiumId,
        Long commonAreaId,
        String commonAreaName,
        String title,
        String description,
        String imagePath,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public CondominiumGalleryDTO(CondominiumGalleryImage entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getCommonArea() != null ? entity.getCommonArea().getId() : null,
                entity.getCommonArea() != null ? entity.getCommonArea().getName() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getImagePath(),
                entity.getUploadedBy() != null ? entity.getUploadedBy().getId() : null,
                entity.getUploadedBy() != null
                        ? (entity.getUploadedBy().getFirstName() + " " + entity.getUploadedBy().getLastName()).trim()
                        : "Administração",
                entity.getCreatedAt()
        );
    }

    public record CreateGalleryImageDTO(
            Long commonAreaId,
            @NotBlank String title,
            String description,
            @NotBlank String image
    ) {}
}
