package com.cefet_rj.projeto_final.packages.enums;

import com.cefet_rj.projeto_final.shared.exceptions.InvalidStatusException;

import java.util.Set;

public enum PackageStatus {
	EXPECTED,
	RECEIVED,
	NOTIFIED,
	PICKED_UP,
	CANCELLED,
	RETURNED;

	public boolean canChangeTo(PackageStatus nextStatus) {
		return switch (this) {
			case EXPECTED -> Set.of(RECEIVED, CANCELLED).contains(nextStatus);
			case RECEIVED -> Set.of(NOTIFIED, PICKED_UP, RETURNED).contains(nextStatus);
			case NOTIFIED -> Set.of(PICKED_UP, RETURNED).contains(nextStatus);
			case PICKED_UP, CANCELLED, RETURNED -> false;
		};
	}

	public static PackageStatus fromString(String value) {
		for (PackageStatus status : PackageStatus.values()) {
			if (status.name().equalsIgnoreCase(value)) {
				return status;
			}
		}

		throw new InvalidStatusException("Invalid package status: " + value);
	}
}