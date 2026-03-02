package com.cefet_rj.projeto_final.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PackageStatus {
    PENDING(1L),
    PICKED_UP(2L);

	@Getter
	private final Long id;
}
