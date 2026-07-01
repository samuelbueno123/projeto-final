package com.cefet_rj.projeto_final.condominiums.dtos.update;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;

public record ResidentUpdateDTO (
        Long userId,
        Long unitId,
        String type

){

    public ResidentUpdateDTO(Resident entity) {
        this(
                entity.getUser().getId(),
                entity.getUnit().getId(),
                entity.getType().toString()
        );
    }
}
