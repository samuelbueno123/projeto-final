package com.cefet_rj.projeto_final.condominiums.dtos.update;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;

public record UnitUpdateDTO(
		String identifier,
		Long sectorId
) {

	public UnitUpdateDTO(Unit entity) {
		this(
				entity.getIdentifier(),
				entity.getSector().getId()
		    );
	}
}
