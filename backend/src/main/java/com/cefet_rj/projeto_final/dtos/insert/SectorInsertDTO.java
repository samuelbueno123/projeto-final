package com.cefet_rj.projeto_final.dtos.insert;

import com.cefet_rj.projeto_final.entities.Sector;

public record SectorInsertDTO(
        String name,
		String structuralType,
        Long condominiumId
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
