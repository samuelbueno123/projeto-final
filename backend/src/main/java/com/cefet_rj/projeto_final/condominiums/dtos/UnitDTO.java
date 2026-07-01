package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;

public record UnitDTO(
		Long id,
		String identifier,
		Long sectorId
) {

	public UnitDTO(Unit entity) {
		this(
				entity.getId(),
				entity.getIdentifier(),
				entity.getSector().getId()
		    );
	}
}
