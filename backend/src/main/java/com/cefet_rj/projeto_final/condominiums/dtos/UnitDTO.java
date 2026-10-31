package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;

public record UnitDTO(
		Long id,
		String identifier,
		Long sectorId,
		String sectorName,
		Long condominiumId,
		String condominiumName
) {

	public UnitDTO(Long id, String identifier, Long sectorId) {
		this(id, identifier, sectorId, null, null, null);
	}

	public UnitDTO(Unit entity) {
		this(
				entity.getId(),
				entity.getIdentifier(),
				entity.getSector() != null ? entity.getSector().getId() : null,
				entity.getSector() != null ? entity.getSector().getName() : null,
				entity.getSector() != null && entity.getSector().getCondominium() != null
						? entity.getSector().getCondominium().getId()
						: null,
				entity.getSector() != null && entity.getSector().getCondominium() != null
						? entity.getSector().getCondominium().getName()
						: null
		    );
	}
}
