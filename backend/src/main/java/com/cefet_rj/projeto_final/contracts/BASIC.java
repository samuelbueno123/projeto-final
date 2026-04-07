package com.cefet_rj.projeto_final.contracts;

public class BASIC implements BillingPlanInterface {

	public String getPlanName() {
		return "BASIC";
	}

	@Override
	public void calculateMonthlyFee() {
	}
}
