package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;

/**
 * Morador de uma unidade, exposto à portaria para registro de encomendas.
 */
public record DirectoryResidentDTO(
        Long id,
        Long userId,
        String name,
        String email,
        String type,
        Long unitId,
        String unitIdentifier,
        String sectorName,
        String profileImage
) {
    public DirectoryResidentDTO(Resident entity) {
        this(
                entity.getId(),
                entity.getUser().getId(),
                (entity.getUser().getFirstName() + " " + entity.getUser().getLastName()).trim(),
                entity.getUser().getEmail(),
                entity.getType() == null ? null : entity.getType().name(),
                entity.getUnit().getId(),
                entity.getUnit().getIdentifier(),
                entity.getUnit().getSector().getName(),
                entity.getUser().getProfileImage()
        );
    }
}
