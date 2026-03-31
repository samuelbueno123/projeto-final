package com.cefet_rj.projeto_final.dtos.insert;

import com.cefet_rj.projeto_final.entities.Resident;

public record ResidentInsertDTO (
        Long userId,
        Long unitId,
        String type
){

    public ResidentInsertDTO(Resident entity) {
        this(
                entity.getUser().getId(),
                entity.getUnit().getId(),
                entity.getType().toString()
        );
    }
}