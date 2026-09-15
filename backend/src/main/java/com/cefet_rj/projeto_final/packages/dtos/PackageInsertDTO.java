package com.cefet_rj.projeto_final.packages.dtos;

import com.cefet_rj.projeto_final.packages.entities.Package;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PackageInsertDTO(
        @NotBlank String description,
        @NotNull Long recipientId,
        @NotNull Long unitId
) {

    public PackageInsertDTO(Package entity) {
        this(
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId()
        );
    }
}
