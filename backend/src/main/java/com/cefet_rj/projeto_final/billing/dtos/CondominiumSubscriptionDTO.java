package com.cefet_rj.projeto_final.billing.dtos;

import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.Feature;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CondominiumSubscriptionDTO(
        Long id,
        Long condominiumId,
        String condominiumName,
        Long billingPlanId,
        String billingPlanName,
        String billingPlanCode,
        BigDecimal monthlyFee,
        Integer maxUnits,
        String status,
        String billingCycle,
        Boolean accessAllowed,
        List<String> enabledFeatures,
        LocalDateTime startedAt,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        LocalDateTime trialEndsAt,
        LocalDateTime cancelledAt,
        Boolean autoRenew,
        LocalDateTime updatedAt
) {
    public CondominiumSubscriptionDTO(CondominiumSubscription entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getCondominium().getName(),
                entity.getBillingPlan().getId(),
                entity.getBillingPlan().getName(),
                entity.getBillingPlan().getCode(),
                entity.getBillingPlan().getMonthlyFee(),
                entity.getBillingPlan().getMaxUnits(),
                entity.getStatus().name(),
                entity.getBillingCycle().name(),
                isOperational(entity),
                isOperational(entity) && entity.getBillingPlan().getFeatures() != null
                        ? entity.getBillingPlan().getFeatures().stream()
                        .map(Feature::getCode)
                        .sorted()
                        .toList()
                        : List.of(),
                entity.getStartedAt(),
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.getTrialEndsAt(),
                entity.getCancelledAt(),
                entity.getAutoRenew(),
                entity.getUpdatedAt()
        );
    }

    private static boolean isOperational(CondominiumSubscription s) {
        if (s == null || s.getStatus() == null) return false;
        return switch (s.getStatus()) {
            case ACTIVE, PAST_DUE -> true;
            case TRIAL -> s.getTrialEndsAt() == null || !s.getTrialEndsAt().isBefore(LocalDateTime.now());
            case SUSPENDED, CANCELLED -> false;
        };
    }
}
