package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Sector;

public record SectorDTO(
        Long id,
        String name,
		String structuralType,
        Long condominiumId
) {

    public SectorDTO(Sector entity) {
        this(
                entity.getId(),
                entity.getName(),
				entity.getStructuralType().toString(),
                entity.getCondominium() != null
                        ? entity.getCondominium().getId()
                        : null
        );
    }
}
