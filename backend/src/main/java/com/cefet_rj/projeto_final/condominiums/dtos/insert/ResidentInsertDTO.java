package com.cefet_rj.projeto_final.condominiums.dtos.insert;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResidentInsertDTO(
        @NotNull Long userId,
        @NotNull Long unitId,
        @NotBlank String type
){

    public ResidentInsertDTO(Resident entity) {
        this(
                entity.getUser().getId(),
                entity.getUnit().getId(),
                entity.getType().toString()
        );
    }
}