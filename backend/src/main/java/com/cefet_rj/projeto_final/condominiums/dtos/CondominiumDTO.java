package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;

import java.util.List;

public record CondominiumDTO(
        Long id,
        String name,
        String address,
        Long billingPlanId,
        String billingPlanName,
        Boolean isActive,
        Boolean planSelected,
        List<String> features
) {

    public CondominiumDTO(
            Long id,
            String name,
            String address,
            Long billingPlanId,
            String billingPlanName,
            Boolean isActive
    ) {
        this(id, name, address, billingPlanId, billingPlanName, isActive, false, List.of());
    }

    public CondominiumDTO(
            Long id,
            String name,
            String address,
            Long billingPlanId,
            String billingPlanName,
            Boolean isActive,
            Boolean planSelected
    ) {
        this(id, name, address, billingPlanId, billingPlanName, isActive, planSelected, List.of());
    }

    public CondominiumDTO(Condominium entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getBillingPlan() != null ? entity.getBillingPlan().getId() : null,
                entity.getBillingPlan() != null ? entity.getBillingPlan().getName() : null,
                entity.getIsActive(),
                entity.getPlanSelected() != null ? entity.getPlanSelected() : false,
                entity.getBillingPlan() != null && entity.getBillingPlan().getFeatures() != null
                        ? entity.getBillingPlan().getFeatures().stream().map(Feature::getCode).toList()
                        : List.of()
        );
    }
}

