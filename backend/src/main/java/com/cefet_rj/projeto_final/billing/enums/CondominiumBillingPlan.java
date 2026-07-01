package com.cefet_rj.projeto_final.billing.enums;

import com.cefet_rj.projeto_final.shared.exceptions.InvalidStatusException;

public enum CondominiumBillingPlan {
	BASIC,
	PREMIUM;

	public static CondominiumBillingPlan fromString(String value) {
		for (CondominiumBillingPlan plan : CondominiumBillingPlan.values()) {
			if (plan.name().equalsIgnoreCase(value)) {
				return plan;
			}
		}

		throw new InvalidStatusException("Invalid condominium billing plan: " + value);
	}
}