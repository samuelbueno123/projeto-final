package com.cefet_rj.projeto_final.strategies;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;

import java.math.BigDecimal;

public class PremiumBillingPlan implements BillingPlanInterface {

	@Override
	public String getPlanName() {
		return "PREMIUM";
	}

	@Override
	public BigDecimal calculateMonthlyFee() {

		return BigDecimal.ZERO;
	}

}
