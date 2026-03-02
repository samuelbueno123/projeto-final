package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.CondominiumUser;

public record CondominiumUserDTO (

		Long id,
		Long userId,
		Long condominiumId,
		Long roleId

) {

	public CondominiumUserDTO(CondominiumUser entity) {
		this(
				entity.getId(),
				entity.getUser().getId(),
				entity.getCondominium().getId(),
				entity.getRole().getId()
		);
	}
}
