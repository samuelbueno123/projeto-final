package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.billing.enums.CondominiumBillingPlan;

import java.util.Set;
import java.util.stream.Collectors;

public record CondominiumDTO(
        Long id,
        String name,
        String address,
        CondominiumBillingPlan billingPlan,
        Boolean isActive,
        Set<SectorDTO> sectors
) {

    public CondominiumDTO(Condominium entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getBillingPlan(),
                entity.getIsActive(),
                entity.getSectors()
                        .stream()
                        .map(SectorDTO::new)
                        .collect(Collectors.toSet())
        );
    }
}
