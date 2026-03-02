package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Condominium;

import java.util.Set;
import java.util.stream.Collectors;

public record CondominiumDTO(
        Long id,
        String name,
        String address,
        String billingPlan,
        Boolean isActive,
        Set<SectorDTO> sectors
) {

    public CondominiumDTO(Condominium entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getBillingPlan().name(),
                entity.getIsActive(),
                entity.getSectors()
                        .stream()
                        .map(SectorDTO::new)
                        .collect(Collectors.toSet())
        );
    }
}
