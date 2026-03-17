package com.cefet_rj.projeto_final.interfaces;

import lombok.Getter;
import lombok.Setter;

public class BasicBillingPlan implements CondominiumBillingPlan {

	@Getter @Setter
	private Double monthlyFee;

	@Getter @Setter
	private Double pickupFee;

	public BasicBillingPlan() {
		this.monthlyFee = 100.0; // Exemplo de taxa mensal fixa
		this.pickupFee = 10.0;   // Exemplo de taxa por coleta
	}

	@Override
	public Boolean isBasicPlan() { return true; }

	@Override
	public Boolean isPremiumPlan() { return false; }
	
}
