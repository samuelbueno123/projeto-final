package com.cefet_rj.projeto_final.contracts;

import java.math.BigDecimal;

public class BASIC implements BillingPlanInterface {

	@Override
	public String getPlanName() {
		return "BASIC";
	}

	@Override
	public BigDecimal calculateMonthlyFee() {

		return BigDecimal.ZERO;
	}
}
