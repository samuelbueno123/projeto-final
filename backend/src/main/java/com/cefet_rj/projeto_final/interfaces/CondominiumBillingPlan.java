package com.cefet_rj.projeto_final.interfaces;

public interface CondominiumBillingPlan {

		Boolean isBasicPlan();
		Boolean isPremiumPlan();

		Double getMonthlyFee();
		Double getPickupFee();

}
