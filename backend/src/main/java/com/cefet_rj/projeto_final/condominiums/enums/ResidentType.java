package com.cefet_rj.projeto_final.condominiums.enums;

import com.cefet_rj.projeto_final.shared.exceptions.InvalidStatusException;

public enum ResidentType {
	TENANT,
	DEPENDENT,
	OWNER;

	public static ResidentType fromString(String value) {
		for (ResidentType type : ResidentType.values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}

		throw new InvalidStatusException("Invalid resident type: " + value);
	}
}


