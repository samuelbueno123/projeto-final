package com.cefet_rj.projeto_final.packages.history.dtos;

import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.history.entities.PackageHistory;
import com.cefet_rj.projeto_final.packages.history.enums.PackageHistoryActorType;

import java.time.LocalDateTime;

public record PackageHistoryDTO(
        Long id,
        PackageStatus previousStatus,
        PackageStatus newStatus,
        Long performedById,
        String performedByName,
        PackageHistoryActorType actorType,
        LocalDateTime performedAt,
        String notes
) {

    public PackageHistoryDTO(PackageHistory entity) {
        this(
                entity.getId(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),

                entity.getPerformedBy() != null
                        ? entity.getPerformedBy().getId()
                        : null,

                entity.getPerformedBy() != null
                        ? entity.getPerformedBy().getFirstName()
                        + " "
                        + entity.getPerformedBy().getLastName()
                        : null,

                entity.getActorType(),
                entity.getPerformedAt(),
                entity.getNotes()
        );
    }
}