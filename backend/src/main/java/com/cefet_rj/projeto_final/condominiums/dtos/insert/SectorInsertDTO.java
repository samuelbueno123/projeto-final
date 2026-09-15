package com.cefet_rj.projeto_final.condominiums.dtos.insert;

import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SectorInsertDTO(
        @NotBlank String name,
        @NotBlank String structuralType,
        @NotNull Long condominiumId
) {

    public SectorInsertDTO(Sector entity) {
        this(
                entity.getName(),
				entity.getStructuralType().toString(),
                entity.getCondominium() != null
                        ? entity.getCondominium().getId()
                        : null
        );
    }
}
