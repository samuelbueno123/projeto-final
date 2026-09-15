package com.cefet_rj.projeto_final.packages.dtos;

import com.cefet_rj.projeto_final.packages.entities.Package;

public record PackageUpdateDTO(
        String description,
        Long recipientId,
        Long unitId
) {

    public PackageUpdateDTO(Package entity) {
        this(
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId()
        );
    }
}
