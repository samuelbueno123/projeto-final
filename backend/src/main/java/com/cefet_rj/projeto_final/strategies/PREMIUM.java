package com.cefet_rj.projeto_final.contracts;

import java.math.BigDecimal;

public class PREMIUM implements BillingPlanInterface {

	@Override
	public String getPlanName() {
		return "PREMIUM";
	}

	@Override
	public BigDecimal calculateMonthlyFee() {

		return BigDecimal.ZERO;
	}

}
