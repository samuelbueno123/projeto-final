package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Package;

import java.time.LocalDateTime;

public record PackageDTO(
        Long id,
        String description,
        Long recipientId,
        Long unitId,
        String status,
        LocalDateTime arrivalAt,
        LocalDateTime pickedUpAt
) {

    public PackageDTO(Package entity) {
        this(
                entity.getId(),
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId(),
                entity.getStatus().name(),
                entity.getArrivalAt(),
                entity.getPickedUpAt()
        );
    }
}
