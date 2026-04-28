package com.cefet_rj.projeto_final.dtos.insert;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan;

import java.util.Set;
import java.util.stream.Collectors;

public record CondominiumInsertDTO(
        String name,
        String address,
        CondominiumBillingPlan billingPlan,
        Boolean isActive,
        Set<SectorDTO> sectors
) {

    public CondominiumInsertDTO(Condominium entity) {
        this(
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
