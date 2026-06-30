package com.cefet_rj.projeto_final.dtos.update;

import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;

import java.time.LocalDateTime;

public record PackageUpdateDTO(
        String description,
        Long recipientId,
        Long unitId,
        PackageStatus status,
        LocalDateTime arrivalAt,
        LocalDateTime pickedUpAt,
        String marketplaceCode,
        String marketplaceOrderId
) {

    public PackageUpdateDTO(Package entity) {
        this(
                entity.getDescription(),
                entity.getRecipient().getId(),
                entity.getUnit().getId(),
                entity.getStatus(),
                entity.getArrivalAt(),
                entity.getPickedUpAt(),
                entity.getMarketplaceCode(),
                entity.getMarketplaceOrderId()
        );
    }
}
