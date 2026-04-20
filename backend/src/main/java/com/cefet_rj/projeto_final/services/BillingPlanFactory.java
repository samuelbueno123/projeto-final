package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;

public class BillingPlanFactory {

	public static BillingPlanInterface getPlan(BillingPlanInterface plan) {
		return switch (plan.getPlanName()) {
			case "BASIC" -> new BasicPlan();
			case "PREMIUM" -> new PremiumPlan();
		};
	}
}
