package com.cefet_rj.projeto_final.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SectorStructuralType {
    BLOCK(1L),
    STREET(2L),
    COURT(3L),
    GRILL(4L);

	@Getter
	private final Long id;
}
