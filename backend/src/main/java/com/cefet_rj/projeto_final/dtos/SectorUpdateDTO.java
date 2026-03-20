package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Sector;

public record SectorUpdateDTO(
        String name,
		String structuralType,
        Long condominiumId
) {

    public SectorUpdateDTO(Sector entity) {
        this(
                entity.getName(),
				entity.getStructuralType().toString(),
                entity.getCondominium() != null
                        ? entity.getCondominium().getId()
                        : null
        );
    }
}
