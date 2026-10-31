package com.cefet_rj.projeto_final.packages.dtos;

import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;

import java.time.LocalDateTime;

public record PackageDTO(
        Long id,
        Long condominiumId,
        String condominiumName,
        String description,
        Long recipientId,
        String recipientName,
        Long unitId,
        String unitIdentifier,
        String sectorName,
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
                entity.getUnit() != null && entity.getUnit().getSector() != null
                        ? entity.getUnit().getSector().getCondominium().getId()
                        : null,
                entity.getUnit() != null && entity.getUnit().getSector() != null
                        ? entity.getUnit().getSector().getCondominium().getName()
                        : null,
                entity.getDescription(),
                entity.getRecipient().getId(),
                (entity.getRecipient().getFirstName() + " "
                        + entity.getRecipient().getLastName()).trim(),
                entity.getUnit().getId(),
                entity.getUnit().getIdentifier(),
                entity.getUnit().getSector() != null
                        ? entity.getUnit().getSector().getName()
                        : null,
                entity.getStatus(),
                entity.getReceivedAt(),
                entity.getNotifiedAt(),
                entity.getPickedUpAt(),
                entity.getCancelledAt(),
                entity.getReturnedAt()
        );
    }
}
