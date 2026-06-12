package com.cefet_rj.projeto_final.entities.enums;

import com.cefet_rj.projeto_final.services.exceptions.InvalidStatusException;

public enum PackageStatus {
	EXPECTED,
	PENDING,
	PICKED_UP;

	public static PackageStatus fromString(String value) {

		for (PackageStatus status : PackageStatus.values()) {
			if (status.name().equalsIgnoreCase(value)) {
				return status;
			}
		}

		throw new InvalidStatusException("Invalid package status: " + value);
	}
}