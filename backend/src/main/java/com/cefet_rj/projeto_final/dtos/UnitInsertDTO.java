package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Unit;

public record UnitInsertDTO(
		String identifier,
		Long sectorId
) {

	public UnitInsertDTO(Unit entity) {
		this(
				entity.getIdentifier(),
				entity.getSector().getId()
		    );
	}
}
