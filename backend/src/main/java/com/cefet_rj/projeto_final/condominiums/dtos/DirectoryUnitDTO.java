package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;

/**
 * Unidade no formato de diretório da portaria: setor (bloco, rua, quadra...) + identificador.
 */
public record DirectoryUnitDTO(
        Long id,
        String identifier,
        Long sectorId,
        String sectorName,
        String sectorType
) {
    public DirectoryUnitDTO(Unit entity) {
        this(
                entity.getId(),
                entity.getIdentifier(),
                entity.getSector().getId(),
                entity.getSector().getName(),
                entity.getSector().getStructuralType().name()
        );
    }
}
