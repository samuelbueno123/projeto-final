package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.strategies.BasicBillingPlan;
import com.cefet_rj.projeto_final.strategies.PremiumBillingPlan;

public class BillingPlanFactory {

	public static BillingPlanInterface create(CondominiumBillingPlan plan) {
		return switch (plan.name()) {
			case "BASIC" -> new BasicBillingPlan();
			case "PREMIUM" -> new PremiumBillingPlan();
			default -> throw new IllegalStateException("Unexpected value: " + plan.name());
		};
	}
}
