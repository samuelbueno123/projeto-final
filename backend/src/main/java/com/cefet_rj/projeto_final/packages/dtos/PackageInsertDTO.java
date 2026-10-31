package com.cefet_rj.projeto_final.packages.dtos;

import com.cefet_rj.projeto_final.packages.entities.Package;
import jakarta.validation.constraints.NotBlank;

public record PackageInsertDTO(
        Long condominiumId,
        @NotBlank String description,
        Long recipientId,
        Long unitId
) {

    public PackageInsertDTO(
            String description,
            Long recipientId,
            Long unitId
    ) {
        this(null, description, recipientId, unitId);
    }

    public PackageInsertDTO(Package entity) {
        this(
                entity.getUnit() != null && entity.getUnit().getSector() != null
                        ? entity.getUnit().getSector().getCondominium().getId()
                        : null,
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId()
        );
    }
}
