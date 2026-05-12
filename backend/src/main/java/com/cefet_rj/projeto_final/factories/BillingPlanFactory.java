package com.cefet_rj.projeto_final.factories;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.strategies.BasicBillingPlan;
import com.cefet_rj.projeto_final.strategies.PremiumBillingPlan;

public class BillingPlanFactory {

	public static BillingPlanInterface create(CondominiumBillingPlan plan) {
		return switch (plan) {
			case BASIC -> new BasicBillingPlan();
			case PREMIUM -> new PremiumBillingPlan();
		};
	}
}
