package com.cefet_rj.projeto_final.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CondominiumBillingPlan {
    BASIC(1L),
    PREMIUM(2L);

	@Getter
	private final Long id;
}
