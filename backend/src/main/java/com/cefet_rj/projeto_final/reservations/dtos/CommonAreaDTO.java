package com.cefet_rj.projeto_final.reservations.dtos;

import com.cefet_rj.projeto_final.reservations.entities.CommonArea;

public record CommonAreaDTO(

        Long id,
        String name,
        String description,
        Integer capacity,
        Boolean active,
        Boolean requiresApproval,
        String rules,
        Integer openingHour,
        Integer closingHour,
        Long condominiumId,
        String condominiumName

) {

    public CommonAreaDTO(CommonArea entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacity(),
                entity.getActive(),
                entity.getRequiresApproval() != null ? entity.getRequiresApproval() : true,
                entity.getRules(),
                entity.getOpeningHour() != null ? entity.getOpeningHour() : 8,
                entity.getClosingHour() != null ? entity.getClosingHour() : 22,
                entity.getCondominium().getId(),
                entity.getCondominium().getName()
        );
    }
}
