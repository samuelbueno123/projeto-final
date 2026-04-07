package com.cefet_rj.projeto_final.contracts;

public class PREMIUM implements BillingPlanInterface {

	public String getPlanName() {
		return "PREMIUM";
	}

	@Override
	public void calculateMonthlyFee() {
	}

}
