package com.cefet_rj.projeto_final.condominiums.dtos.update;

import com.cefet_rj.projeto_final.condominiums.entities.Sector;

public record SectorUpdateDTO(
        String name,
        String structuralType,
        Long condominiumId,
        String imageUrl
) {

    public SectorUpdateDTO(String name, String structuralType, Long condominiumId) {
        this(name, structuralType, condominiumId, null);
    }

    public SectorUpdateDTO(Sector entity) {
        this(
                entity.getName(),
                entity.getStructuralType().toString(),
                entity.getCondominium() != null
                        ? entity.getCondominium().getId()
                        : null,
                entity.getImageUrl()
        );
    }
}
