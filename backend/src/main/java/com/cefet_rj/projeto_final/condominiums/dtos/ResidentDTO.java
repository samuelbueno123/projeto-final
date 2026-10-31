package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;

public record ResidentDTO (
		Long id,
		Long userId,
		Long unitId,
		String type,
		String userName,
		String userEmail,
		String unitIdentifier,
		Long condominiumId,
		String condominiumName
){

	public ResidentDTO(Long id, Long userId, Long unitId, String type) {
		this(id, userId, unitId, type, null, null, null, null, null);
	}

	public ResidentDTO(Resident entity) {
		this(
				entity.getId(),
				entity.getUser().getId(),
				entity.getUnit().getId(),
				entity.getType().toString(),
				(entity.getUser().getFirstName() + " " + entity.getUser().getLastName()).trim(),
				entity.getUser().getEmail(),
				entity.getUnit().getIdentifier(),
				entity.getUnit().getSector() != null && entity.getUnit().getSector().getCondominium() != null
						? entity.getUnit().getSector().getCondominium().getId()
						: null,
				entity.getUnit().getSector() != null && entity.getUnit().getSector().getCondominium() != null
						? entity.getUnit().getSector().getCondominium().getName()
						: null
		    );
	}
}
