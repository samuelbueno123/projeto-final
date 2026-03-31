package com.cefet_rj.projeto_final.dtos.update;

import com.cefet_rj.projeto_final.entities.Unit;

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
