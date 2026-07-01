package com.cefet_rj.projeto_final.billing.strategies;

import com.cefet_rj.projeto_final.billing.contracts.BillingPlanInterface;

import java.math.BigDecimal;

public class BasicBillingPlan implements BillingPlanInterface {

	@Override
	public String getPlanName() {
		return "BASIC";
	}

	@Override
	public BigDecimal calculateMonthlyFee() {

		return BigDecimal.ZERO;
	}
}
