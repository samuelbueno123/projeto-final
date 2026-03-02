package com.cefet_rj.projeto_final.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ResidentType {
	TENANT(1L),
	DEPENDENT(2L),
	OWNER(3L);

	@Getter
	private final Long id;
}


