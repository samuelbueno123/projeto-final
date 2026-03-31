package com.cefet_rj.projeto_final.dtos.update;

import com.cefet_rj.projeto_final.entities.Package;

import java.time.LocalDateTime;

public record PackageUpdateDTO(
        String description,
        Long recipientId,
        Long unitId,
        String status,
        LocalDateTime arrivalAt,
        LocalDateTime pickedUpAt
) {

    public PackageUpdateDTO(Package entity) {
        this(
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId(),
                entity.getStatus().name(),
                entity.getArrivalAt(),
                entity.getPickedUpAt()
        );
    }
}
