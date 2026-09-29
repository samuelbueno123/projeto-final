package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;

public record CondominiumDTO(
        Long id,
        String name,
        String address,
        Long billingPlanId,
        String billingPlanName,
        Boolean isActive
) {

    public CondominiumDTO(Condominium entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getBillingPlan().getId(),
                entity.getBillingPlan().getName(),
                entity.getIsActive()
        );
    }
}