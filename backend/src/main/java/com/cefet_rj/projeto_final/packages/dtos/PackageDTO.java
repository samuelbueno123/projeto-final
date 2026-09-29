package com.cefet_rj.projeto_final.packages.dtos;

import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;

import java.time.LocalDateTime;

public record PackageDTO(
        Long id,
        String description,
        Long recipientId,
        Long unitId,
        PackageStatus status,
        LocalDateTime receivedAt,
        LocalDateTime notifiedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime cancelledAt,
        LocalDateTime returnedAt
) {

    public PackageDTO(Package entity) {
        this(
                entity.getId(),
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId(),
                entity.getStatus(),
                entity.getReceivedAt(),
                entity.getNotifiedAt(),
                entity.getPickedUpAt(),
                entity.getCancelledAt(),
                entity.getReturnedAt()
        );
    }
}
