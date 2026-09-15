package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;

public record MembershipDTO(

		Long id,
		Long userId,
		Long condominiumId,
		String authority

) {

	public MembershipDTO(Membership entity) {
		this(
				entity.getId(),
				entity.getUser().getId(),
				entity.getCondominium().getId(),
				entity.getRole().getAuthority()
		);
	}
}
