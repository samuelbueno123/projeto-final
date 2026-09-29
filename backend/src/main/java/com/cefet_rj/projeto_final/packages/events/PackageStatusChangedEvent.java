package com.cefet_rj.projeto_final.packages.events;

import com.cefet_rj.projeto_final.packages.enums.PackageStatus;

import java.time.LocalDateTime;

public record PackageStatusChangedEvent(
        Long packageId,
        Long recipientId,
        Long condominiumId,
        PackageStatus previousStatus,
        PackageStatus newStatus,
        LocalDateTime occurredAt
) {
}