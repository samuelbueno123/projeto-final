package com.cefet_rj.projeto_final.dtos.insert;

import com.cefet_rj.projeto_final.entities.Membership;

public record MembershipInsertDTO(
		Long userId,
		Long condominiumId,
		String authority
) {

	public MembershipInsertDTO(Membership entity) {
		this(
				entity.getUser().getId(),
				entity.getCondominium().getId(),
				entity.getRole().getAuthority()
		);
	}
}
