package com.cefet_rj.projeto_final.billing.dtos;

import com.cefet_rj.projeto_final.billing.entities.SubscriptionHistory;

import java.time.LocalDateTime;

public record SubscriptionHistoryDTO(
        Long id,
        Long condominiumId,
        String previousPlanName,
        String newPlanName,
        String previousStatus,
        String newStatus,
        String eventType,
        String reason,
        String changedByName,
        LocalDateTime createdAt
) {
    public SubscriptionHistoryDTO(SubscriptionHistory entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getPreviousPlan() != null ? entity.getPreviousPlan().getName() : null,
                entity.getNewPlan() != null ? entity.getNewPlan().getName() : null,
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getEventType(),
                entity.getReason(),
                entity.getChangedBy() != null
                        ? (entity.getChangedBy().getFirstName() + " " + (entity.getChangedBy().getLastName() != null ? entity.getChangedBy().getLastName() : "")).trim()
                        : "Sistema",
                entity.getCreatedAt()
        );
    }
}
