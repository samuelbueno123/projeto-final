package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Resident;

public record ResidentDTO (

		Long id,
		Long userId,
		Long unitId,
		String type

){

	public ResidentDTO(Resident entity) {
		this(
				entity.getId(),
				entity.getUser().getId(),
				entity.getUnit().getId(),
				entity.getType().toString()
		    );
	}

}
