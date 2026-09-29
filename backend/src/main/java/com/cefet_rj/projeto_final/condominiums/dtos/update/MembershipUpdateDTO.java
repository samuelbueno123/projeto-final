package com.cefet_rj.projeto_final.condominiums.dtos.update;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;

public record MembershipUpdateDTO(
		Long userId,
		Long condominiumId,
		String authority
) {

	public MembershipUpdateDTO(Membership entity) {
		this(
				entity.getUser().getId(),
				entity.getCondominium().getId(),
				entity.getRole().getAuthority()
		);
	}
}
