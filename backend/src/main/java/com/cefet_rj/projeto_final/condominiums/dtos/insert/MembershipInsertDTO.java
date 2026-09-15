package com.cefet_rj.projeto_final.condominiums.dtos.insert;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MembershipInsertDTO(
		@NotNull Long userId,
		@NotNull Long condominiumId,
		@NotBlank String authority
) {

	public MembershipInsertDTO(Membership entity) {
		this(
				entity.getUser().getId(),
				entity.getCondominium().getId(),
				entity.getRole().getAuthority()
		);
	}
}
