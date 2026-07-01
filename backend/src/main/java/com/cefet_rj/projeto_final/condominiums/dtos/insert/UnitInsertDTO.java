package com.cefet_rj.projeto_final.condominiums.dtos.insert;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UnitInsertDTO(
		@NotBlank String identifier,
		@NotNull Long sectorId
) {

	public UnitInsertDTO(Unit entity) {
		this(
				entity.getIdentifier(),
				entity.getSector().getId()
		    );
	}
}
