package com.cefet_rj.projeto_final.billing.factories;

import com.cefet_rj.projeto_final.billing.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.billing.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.billing.strategies.BasicBillingPlan;
import com.cefet_rj.projeto_final.billing.strategies.PremiumBillingPlan;

public class BillingPlanFactory {

	public static BillingPlanInterface create(CondominiumBillingPlan plan) {
		return switch (plan) {
			case BASIC -> new BasicBillingPlan();
			case PREMIUM -> new PremiumBillingPlan();
		};
	}
}
